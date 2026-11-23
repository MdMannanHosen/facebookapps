# Security model

## Secrets

| Secret | Where it lives | In the app? |
|--------|----------------|-------------|
| Supabase anon (public) key | `BuildConfig.SUPABASE_ANON_KEY` | ✅ (public by design) |
| Supabase service-role key | Server / n8n only | ❌ never |
| Payment gateway secret | Server (Edge Function) | ❌ never |
| WhatsApp access token | Server (n8n / Edge Function) | ❌ never |
| User password | Supabase Auth | ❌ never stored locally |

## Authentication & sessions

- Supabase Auth (GoTrue) via `/auth/v1/token`.
- Access + refresh tokens stored in DataStore (`SessionManager`).
- Access token attached as `Authorization: Bearer` by `SupabaseClient`.
- `refreshSession()` rotates tokens using the stored refresh token.
- No passwords are persisted on the device.

## Authorization (defense in depth)

1. **Supabase RLS** — the authoritative boundary.
2. **Server-side RPCs** — `create_membership` / `record_payment` verify `is_staff()`
   and recompute financial values.
3. **Client gating** — `CheckStaffPermission` and role checks hide actions the user
   cannot perform (UX only, never trusted).

## Payment integrity

- The client **never** decides whether a payment succeeded.
- Cash is `SUCCESS` server-side; online payments remain `PENDING` until verified
  by a signed webhook handled by `supabase/functions/verify-payment`.
- Every payment carries an `idempotency_key`; retries return the original row.
- Overpayment above the outstanding due is rejected in the RPC.
- Refunds are staff-only (`refund_payment` requires `is_staff()`), capped at
  `paid - already refunded`, and appended as a new `REFUNDED` row.
- Payment history is append-only; no silent deletes.

## Staff permissions

- Owner and staff are distinguished by `profiles.role`.
- `staff_permissions` rows are writable only by owners (RLS `staff_permissions_write`).
- The app mirrors the grants in `CheckStaffPermission` for UI gating only; every
  RPC and policy re-checks them server-side.

## Webhook verification (server-side)

- Verify the gateway signature (HMAC) before marking a payment `SUCCESS`.
- Re-check the `idempotency_key` / transaction id to avoid duplicates.
- Only the Edge Function with the gateway secret can call the verification path.

## Data protection

- Members can read only their own membership/payment data (RLS).
- The `member_overview` view is declared `WITH (security_invoker = true)` so the
  caller's RLS policies apply. Without this, a view runs with its owner's
  privileges and would leak every member's profile and dues to any signed-in
  user.
- `members.id` mirrors `auth.users.id`: the `on_auth_user_created` trigger inserts
  a member row for every signup, which is what the self-service RLS policies
  (`id = auth.uid()`) rely on.
- Audit logs record who created memberships and recorded payments.
- Logs must not contain passwords, tokens or full card data.
- `allowBackup=false` prevents local DB/token extraction via backups.

## Checklist for production

- [ ] Rotate the anon key if leaked (anon keys are public; RLS must hold).
- [ ] Enable Supabase Auth email confirmation and rate limits.
- [ ] Add a CHECK constraint / trigger for any new financial column.
- [ ] Verify webhook signatures in the Edge Function.
- [ ] Add audit logging for staff permission changes.
- [ ] Review RLS policies after adding any new table.