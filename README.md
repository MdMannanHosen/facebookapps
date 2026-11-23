# GymFlow — Native Android Gym Management System

A production-oriented, native Android application for managing a single gym's
members, memberships, payments, dues and daily reporting. Built with Kotlin,
Jetpack Compose, Material 3, Clean Architecture, MVVM, Hilt and Supabase.

> Brand direction: **Premium dark fitness + clean business dashboard** — charcoal
> surfaces, electric-lime accents, and accessible, status-aware components.

---

## 1. Feature overview

| Area | Owner / Staff | Member |
|------|---------------|--------|
| Authentication (Supabase Auth) | ✅ | ✅ |
| Dashboard (daily KPIs) | ✅ | ✅ (personal) |
| Member registration / edit / delete | ✅ | — |
| Member search & filters | ✅ | — |
| Configurable membership plans | ✅ | — |
| Create membership | ✅ | — |
| Renew membership (incl. early renewal) | ✅ | ✅ (self) |
| Automatic expiry calculation | ✅ | ✅ |
| Receive payment (cash / bKash / Nagad / card / bank) | ✅ | — |
| Partial payments & outstanding dues | ✅ | ✅ (view) |
| Refunds (append-only reversal) | ✅ | — |
| Payment history | ✅ | ✅ (own) |
| Expiring memberships | ✅ | — |
| Notifications | ✅ | ✅ |
| Daily business report + CSV export | ✅ | — |
| Staff permissions | ✅ | — |
| Settings + theme (light/dark/system) | ✅ | ✅ |

Online payment is scaffolded (gateway checkout is a server-side integration) and
clearly marked as "coming soon" in the UI rather than faking success.

---

## 2. Architecture

```
com.mannanhosen.gymflow
├── GymFlowApplication.kt        @HiltAndroidApp
├── MainActivity.kt              @AndroidEntryPoint, hosts Compose + NavHost
├── core/
│   ├── di/                      Hilt modules (network, repositories, dispatchers)
│   ├── network/                 Supabase REST client (OkHttp + kotlinx.serialization)
│   ├── session/                 SessionManager (DataStore) + UserSession
│   └── util/                    Money, AppClock, Validators, NetworkResult
├── data/
│   ├── remote/
│   │   ├── dto/                 REST DTOs (snake_case, @Serializable)
│   │   ├── mapper/              DTO <-> domain mapping
│   │   └── SupabaseAuthApi.kt   GoTrue auth calls
│   └── repository/              Repository implementations
├── domain/
│   ├── model/                   Domain models + enums
│   ├── repository/              Repository interfaces
│   └── usecase/                 Business rules (expiry, due, renewal, payment)
└── presentation/
    ├── navigation/              Routes
    ├── components/              Reusable Compose components
    ├── auth/ dashboard/ members/ membership/ payment/ dues/ reports/
    ├── plans/ notifications/ settings/ member/
    └── util/                    UiEffect, error -> string mapping
```

**Rules enforced**

- UI talks only to ViewModels; ViewModels talk to use cases/repositories.
- Business logic (expiry, due, renewal) lives in `domain/usecase` and is unit tested.
- API models (`dto`) are separate from domain models; mapping is explicit.
- The UI never talks to Supabase directly.
- Financial calculations are validated on the backend (RPCs) and only displayed
  on the client.

---

## 3. Tech stack

- Kotlin 2.2, Jetpack Compose (Material 3), Navigation Compose
- MVVM + Clean Architecture, Hilt (DI), Coroutines + StateFlow
- Retrofit-free REST via OkHttp + kotlinx.serialization
- Supabase (Auth, Postgres, RLS, RPCs)
- DataStore (session), WorkManager dependency reserved for sync workers
- JUnit4 + MockK + Turbine for tests

---

## 4. Getting started

### Prerequisites
- Android Studio (latest stable) with JDK 17
- Supabase project

### Configure Supabase
1. Create a Supabase project.
2. Run `supabase/migrations/0001_init.sql` in the SQL editor (or `supabase db push`).
3. Copy the **Project URL** and **anon (public) key**.
4. Set them in `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "SUPABASE_URL", "\"https://<ref>.supabase.co\"")
   buildConfigField("String", "SUPABASE_ANON_KEY", "\"<anon-key>\"")
   ```
   Never put the service-role key in the app.

### Build
```bash
./gradlew assembleDebug
```
Install on a device/emulator (minSdk 24).

### First run
- Create an account from the login screen (Sign up). New users are `MEMBER`
  by default (via the `handle_new_user` trigger).
- Promote the first user to `OWNER` in the Supabase dashboard:
  ```sql
  update public.profiles set role = 'OWNER' where email = 'you@example.com';
  ```

---

## 5. Design system

- Colors: `ui/theme/Color.kt` (brand lime `#C6F135`, charcoal `#101214`, semantic
  success/warning/error/info). Material 3 color roles + `StatusColors` composition
  local. No hardcoded colors in screens.
- Typography: `ui/theme/Type.kt` (Inter-ready scale, `tnum` numeric style for money).
- Shapes: `ui/theme/Shape.kt` (cards 16dp, buttons 28dp, fields 12dp).
- Spacing: `ui/theme/Spacing.kt` (8dp system) + `TouchTarget` (48/56dp).
- Theme: `ui/theme/Theme.kt` with light/dark schemes and `ThemeMode`.

Reusable components live in `presentation/components`:
`AppTopBar, PrimaryButton, SecondaryButton, DestructiveButton, GymCard, StatCard,
MemberListItem, PaymentListItem, MembershipStatusChip, PaymentStatusChip,
DueAmountBadge, EmptyState, LoadingState, ErrorState, SkeletonLoader,
ConfirmationDialog, GymBottomSheet, PullToRefreshContainer, SearchBar,
AmountInputField, PhoneInputField, DatePickerField, SectionHeader, InfoBanner,
PlanCard, MembershipItem`.

---

## 6. Data model

See `docs/DATABASE.md` and `supabase/migrations/0001_init.sql`.

Core tables: `profiles, members, membership_plans, memberships, payments,
payment_events, notifications, gym_settings, audit_logs, staff_permissions,
integration_outbox` + `member_overview` view.

Key rules implemented in SQL:
- `create_membership(...)`: computes start/end/fee from the plan (early renewal
  aware), writes an audit log.
- `record_payment(...)`: idempotent via `idempotency_key`, rejects overpayment,
  marks cash as `SUCCESS` and online as `PENDING`, writes `payment_events` and a
  WhatsApp outbox row.
- `refund_payment(...)`: appends a `REFUNDED` row (the ledger is never mutated),
  caps the refund at `paid - already refunded`, writes `payment_events` and a
  `refund_receipt` outbox row. `member_overview` adds REFUNDED rows back into the
  due amount automatically.
- RLS: members read only their own data; staff/owner are gated by role; owners
  manage plans/settings/staff permissions.

---

## 7. Testing

Unit tests (JVM) cover the financial/time rules:

```bash
./gradlew test
```

- `CalculateMembershipStatusTest`
- `CalculateDueTest`
- `CalculateRenewalRangeTest`
- `RecordPaymentTest`
- `CheckStaffPermissionTest`

> Tests are provided and runnable; they were not executed in the environment that
> generated this project (no JDK/Gradle available there).

---

## 8. Security

See `docs/SECURITY.md`. Highlights:
- Only the anon key ships; RLS is authoritative.
- No service-role keys, gateway secrets or WhatsApp tokens in the app.
- Passwords handled by Supabase Auth (never stored locally).
- Payment success is never trusted from the client; RPC validates.
- Idempotent financial writes prevent duplicates on retry.
- Audit logs for membership/payment mutations.

---

## 9. Automation (n8n)

See `docs/N8N_WORKFLOWS.md` and `workflows/`. The `integration_outbox` table is the
reliable queue consumed by n8n for WhatsApp receipts, reminders and Google Sheets sync.

---

## 10. Known limitations / next steps

- Online payment (SSLCommerz) checkout is a server-side integration; the UI shows
  a placeholder and never simulates success. `supabase/functions/verify-payment/index.ts`
  is the skeleton that settles a payment from a signed webhook.
- Report export produces CSV (opens in Excel/Sheets); a pixel-perfect PDF is not included.
- Offline mode is intentionally not implemented; the app shows loading/error/retry
  states and never replays financial writes automatically.
- Font uses the system default until Inter font files are bundled (`Type.kt`).
- Deep links / push (FCM) plumbing is reserved but not wired.
- A member cannot renew their own membership: `create_membership` requires `is_staff()`,
  so an owner or staff member must complete the renewal.

---

## 11. Project layout

```
app/                     Android module
supabase/migrations/     SQL schema, RLS, RPCs
workflows/               n8n workflow JSON examples
docs/                    Architecture, database, security, n8n guides
```