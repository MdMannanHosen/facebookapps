# Database guide

All schema, RLS policies and business RPCs live in
[`supabase/migrations/0001_init.sql`](../supabase/migrations/0001_init.sql).

## Tables

| Table | Purpose | Key constraints |
|-------|---------|-----------------|
| `profiles` | Auth user + app role | PK = `auth.users.id`, role ∈ {OWNER, STAFF, MEMBER} |
| `members` | Member profile | PK = `members.id` (mirrors `auth.users.id`); unique `phone` (nullable) |
| `membership_plans` | Configurable plans | `duration_months > 0`, `fee >= 0` |
| `memberships` | Membership records | `end_date >= start_date`, FK to member/plan |
| `payments` | Append-only ledger | unique `idempotency_key`, `amount > 0` |
| `payment_events` | Payment lifecycle | FK to payment |
| `notifications` | In-app notifications | type enum |
| `gym_settings` | Key/value config | unique `key` |
| `staff_permissions` | Per-staff grants | PK = staff profile id |
| `audit_logs` | Mutation audit | actor/action/entity |
| `integration_outbox` | n8n delivery queue | status ∈ {PENDING, SENT, FAILED} |

## View

`member_overview` returns one row per member with:

- `member` (jsonb) — the member profile
- `current_membership` (jsonb | null) — latest membership
- `due_amount` (numeric) — `max(fee - successful payments + refunds, 0)`

The Android `MemberOverviewDto` maps this shape directly.

The view is declared `WITH (security_invoker = true)` so the caller's RLS policies
apply to the underlying tables. Omitting this would run the view with its owner's
privileges and expose every member to any signed-in user.

## Identity link

`auth.users` → `profiles` and `members` rows are created together by the
`on_auth_user_created` trigger, so `members.id == auth.uid()`. This is what the
self-service policies (`members_select_self`, `memberships_select_self`,
`payments_select_self`) depend on. Members created from a signup have a `null`
phone until staff record one.

## Business rules in SQL

### `create_membership(p_member_id, p_plan_id, p_start_date, p_start_after_current)`
- Reads the plan (never trusts client fee).
- Early renewal: if `p_start_after_current` and an unexpired membership exists,
  start = `max(end_date) + 1` day (no lost days).
- Otherwise start = `greatest(p_start_date, current_date)`.
- end = `start + duration_months - 1 day`.
- Writes `memberships` + `audit_logs`.

### `record_payment(p_member_id, p_membership_id, p_amount, p_method, p_reference, p_note, p_idempotency_key, p_collected_by)`
- Idempotent: returns the existing row if `idempotency_key` was used.
- Rejects non-positive amounts and amounts above the outstanding due.
- Cash → `SUCCESS`; online/mobile money → `PENDING` until verified.
- Inserts `payment_events` (`CREATED`, `VERIFIED` when successful).
- Writes an `audit_logs` row and an `integration_outbox` row (`payment_receipt`)
  so n8n can send the WhatsApp receipt.

## Time handling

- `timestamptz` everywhere (UTC).
- Business "today" and schedules use `Asia/Dhaka` on the client (`AppClock`).
- `date` columns (`joined_at`, `start_date`, `end_date`) are business dates.

## RLS summary

- `is_owner()` / `is_staff()` / `current_role()` helpers (security definer).
- Members can read only their own `members`, `memberships`, `payments`.
- Staff can manage members, memberships and payments.
- Owners manage plans, settings and staff permissions.
- `integration_outbox` is server-only.

## Extending the schema

- Add new migrations as `supabase/migrations/0002_*.sql`; never edit applied ones.
- Keep backward compatibility: additive columns, nullable first, then backfill.
- New financial fields must be validated in RPCs, not the client.