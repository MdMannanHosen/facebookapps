// ============================================================================
// GymFlow — verify-payment Edge Function (server-side only)
//
// Deploy:
//   supabase functions deploy verify-payment
//
// Secrets (never shipped in the app):
//   supabase secrets set GATEWAY_WEBHOOK_SECRET=...
//
// Why this exists:
//   The client is never trusted to decide whether an online payment succeeded.
//   The gateway calls this function on a verified webhook; the function checks
//   the HMAC signature, then marks the matching payments row SUCCESS exactly
//   once. Cash payments never come through here.
// ============================================================================

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const WEBHOOK_SECRET = Deno.env.get("GATEWAY_WEBHOOK_SECRET")!;

const admin = createClient(SUPABASE_URL, SERVICE_ROLE_KEY, {
  auth: { persistSession: false },
});

type GatewayPayload = {
  /** Our own idempotency key, sent by the app when the payment was created. */
  idempotency_key?: string;
  /** Gateway transaction id — also unique per attempt. */
  transaction_id?: string;
  amount?: number;
  currency?: string;
  status?: string;
  signature?: string;
};

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** Constant-time HMAC-SHA256 comparison. */
async function signatureIsValid(rawBody: string, provided: string): Promise<boolean> {
  const key = await crypto.subtle.importKey(
    "raw",
    new TextEncoder().encode(WEBHOOK_SECRET),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const mac = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode(rawBody));
  const expected = Array.from(new Uint8Array(mac))
    .map((b) => b.toString(16).padStart(2, "0"))
    .join("");

  if (expected.length !== provided.length) return false;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) {
    diff |= expected.charCodeAt(i) ^ provided.charCodeAt(i);
  }
  return diff === 0;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const rawBody = await req.text();
  let payload: GatewayPayload;
  try {
    payload = JSON.parse(rawBody) as GatewayPayload;
  } catch {
    return json({ error: "invalid_json" }, 400);
  }

  // 1) Authenticate the webhook before trusting anything in the body.
  const provided = payload.signature ?? req.headers.get("x-signature") ?? "";
  if (!provided || !(await signatureIsValid(rawBody, provided))) {
    return json({ error: "invalid_signature" }, 401);
  }

  // 2) Only successful gateway states may settle a payment.
  if (payload.status !== "SUCCESS" && payload.status !== "SETTLED") {
    return json({ skipped: true, reason: "not_successful" });
  }

  if (!payload.idempotency_key && !payload.transaction_id) {
    return json({ error: "missing_reference" }, 400);
  }

  // 3) Find the PENDING row. The RPC enforces the financial rules; the service
  //    role is only used to look the row up and to record the verification.
  const { data: payment, error: findError } = await admin
    .from("payments")
    .select("id, amount, status, member_id, membership_id")
    .or(
      `idempotency_key.eq.${payload.idempotency_key ?? ""},reference.eq.${
        payload.transaction_id ?? ""
      }`,
    )
    .maybeSingle();

  if (findError) return json({ error: "lookup_failed", detail: findError.message }, 500);
  if (!payment) return json({ error: "payment_not_found" }, 404);

  // 4) Idempotent: a second webhook for the same row is a no-op.
  if (payment.status === "SUCCESS") return json({ ok: true, alreadyApplied: true });

  // 5) Amount must match to the paisa; never trust the client's number.
  const expectedAmount = Number(payment.amount);
  if (typeof payload.amount === "number" && Math.abs(payload.amount - expectedAmount) > 0.001) {
    return json({ error: "amount_mismatch" }, 400);
  }

  // 6) Settle: only PENDING -> SUCCESS, and only when the row is still PENDING.
  const { data: updated, error: updateError } = await admin
    .from("payments")
    .update({ status: "SUCCESS", paid_at: new Date().toISOString() })
    .eq("id", payment.id)
    .eq("status", "PENDING")
    .select("id")
    .maybeSingle();

  if (updateError) return json({ error: "update_failed", detail: updateError.message }, 500);

  // 7) Append-only ledger: record the verification for audit/replay.
  if (updated) {
    await admin.from("payment_events").insert({
      payment_id: payment.id,
      type: "VERIFIED",
      amount: expectedAmount,
    });
    await admin.from("audit_logs").insert({
      action: "VERIFY_PAYMENT",
      entity: "payments",
      entity_id: payment.id,
      metadata: {
        transaction_id: payload.transaction_id ?? null,
        source: "edge_function",
      },
    });
    await admin.from("integration_outbox").insert({
      kind: "payment_receipt",
      payload: { payment_id: payment.id, member_id: payment.member_id },
    });
  }

  return json({ ok: true, applied: Boolean(updated) });
});