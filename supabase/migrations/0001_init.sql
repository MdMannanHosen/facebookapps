-- ============================================================================
-- GymFlow — initial schema, RLS policies and business RPCs
-- Run in the Supabase SQL editor or via the Supabase CLI.
-- ============================================================================

create extension if not exists "pgcrypto";

-- ---------------------------------------------------------------------------
-- Helper: updated_at trigger
-- ---------------------------------------------------------------------------
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ---------------------------------------------------------------------------
-- profiles — mirrors auth.users with an application role
-- ---------------------------------------------------------------------------
create table if not exists public.profiles (
  id          uuid primary key references auth.users(id) on delete cascade,
  full_name   text not null default '',
  email       text,
  phone       text,
  role        text not null default 'MEMBER'
              check (role in ('OWNER', 'STAFF', 'MEMBER')),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);

create trigger profiles_updated_at
before update on public.profiles
for each row execute function public.set_updated_at();

-- Auto-create a profile row (and the matching member row) on signup.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, full_name, email, phone, role)
  values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', ''),
    new.email,
    new.raw_user_meta_data ->> 'phone',
    'MEMBER'
  )
  on conflict (id) do nothing;

  -- Mirror the auth user as a member row so that members.id == auth.uid().
  -- Self-service RLS policies rely on this identity link.
  -- The phone is intentionally left null: it stays unique and the signup can
  -- never fail because two members share a phone number. Staff add it later.
  insert into public.members (id, full_name, email, status, joined_at)
  values (
    new.id,
    coalesce(nullif(new.raw_user_meta_data ->> 'full_name', ''), 'Member'),
    new.email,
    'ACTIVE',
    current_date
  )
  on conflict (id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- members
-- ---------------------------------------------------------------------------
create table if not exists public.members (
  id          uuid primary key default gen_random_uuid(),
  full_name   text not null,
  -- Nullable: members created from an auth signup have no phone until staff
  -- record one. The unique constraint still prevents duplicates.
  phone       text unique,
  email       text,
  status      text not null default 'ACTIVE' check (status in ('ACTIVE', 'INACTIVE')),
  joined_at   date not null default current_date,
  photo_url   text,
  created_at  timestamptz not null default now()
);

create index if not exists members_name_idx on public.members (lower(full_name));
create index if not exists members_status_idx on public.members (status);

-- ---------------------------------------------------------------------------
-- membership_plans — configurable, never hardcoded
-- ---------------------------------------------------------------------------
create table if not exists public.membership_plans (
  id               uuid primary key default gen_random_uuid(),
  name             text not null,
  duration_months  integer not null check (duration_months > 0),
  fee              numeric(12, 2) not null check (fee >= 0),
  description      text,
  is_active        boolean not null default true,
  created_at       timestamptz not null default now()
);

-- ---------------------------------------------------------------------------
-- memberships
-- ---------------------------------------------------------------------------
create table if not exists public.memberships (
  id          uuid primary key default gen_random_uuid(),
  member_id   uuid not null references public.members(id) on delete cascade,
  plan_id     uuid not null references public.membership_plans(id),
  plan_name   text not null,
  start_date  date not null,
  end_date    date not null,
  fee         numeric(12, 2) not null check (fee >= 0),
  status      text not null default 'ACTIVE'
              check (status in ('ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'FROZEN', 'PENDING')),
  created_at  timestamptz not null default now(),
  check (end_date >= start_date)
);

create index if not exists memberships_member_idx on public.memberships (member_id, end_date desc);
create index if not exists memberships_end_idx on public.memberships (end_date);

-- ---------------------------------------------------------------------------
-- payments — append-only ledger with idempotency
-- ---------------------------------------------------------------------------
create table if not exists public.payments (
  id               uuid primary key default gen_random_uuid(),
  member_id        uuid not null references public.members(id) on delete restrict,
  membership_id    uuid references public.memberships(id) on delete set null,
  amount           numeric(12, 2) not null check (amount > 0),
  method           text not null check (method in ('CASH', 'ONLINE', 'BKASH', 'NAGAD', 'CARD', 'BANK')),
  status           text not null default 'SUCCESS'
                   check (status in ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
  paid_at          timestamptz,
  reference        text,
  note             text,
  collected_by     uuid references public.profiles(id),
  idempotency_key  text not null unique,
  created_at       timestamptz not null default now()
);

create index if not exists payments_member_idx on public.payments (member_id, created_at desc);
create index if not exists payments_status_idx on public.payments (status);

-- ---------------------------------------------------------------------------
-- payment_events — lifecycle audit for each payment
-- ---------------------------------------------------------------------------
create table if not exists public.payment_events (
  id          uuid primary key default gen_random_uuid(),
  payment_id  uuid not null references public.payments(id) on delete cascade,
  type        text not null check (type in ('CREATED', 'VERIFIED', 'FAILED', 'REFUNDED')),
  amount      numeric(12, 2) not null default 0,
  created_at  timestamptz not null default now()
);

create index if not exists payment_events_payment_idx on public.payment_events (payment_id);

-- ---------------------------------------------------------------------------
-- notifications
-- ---------------------------------------------------------------------------
create table if not exists public.notifications (
  id          uuid primary key default gen_random_uuid(),
  title       text not null,
  body        text not null default '',
  type        text not null default 'GENERAL'
              check (type in ('PAYMENT_DUE', 'MEMBERSHIP_EXPIRING', 'PAYMENT_RECEIVED', 'GENERAL')),
  is_read     boolean not null default false,
  created_at  timestamptz not null default now()
);

create index if not exists notifications_created_idx on public.notifications (created_at desc);

-- ---------------------------------------------------------------------------
-- gym_settings — key/value configuration
-- ---------------------------------------------------------------------------
create table if not exists public.gym_settings (
  id          uuid primary key default gen_random_uuid(),
  key         text not null unique,
  value       jsonb not null default '{}'::jsonb,
  updated_at  timestamptz not null default now()
);

create trigger gym_settings_updated_at
before update on public.gym_settings
for each row execute function public.set_updated_at();

-- ---------------------------------------------------------------------------
-- staff_permissions — per-staff grants
-- ---------------------------------------------------------------------------
create table if not exists public.staff_permissions (
  staff_id               uuid primary key references public.profiles(id) on delete cascade,
  can_manage_members     boolean not null default false,
  can_receive_payments   boolean not null default false,
  can_renew_memberships  boolean not null default false,
  can_view_reports       boolean not null default false,
  can_manage_plans       boolean not null default false,
  can_manage_staff       boolean not null default false
);

-- ---------------------------------------------------------------------------
-- audit_logs
-- ---------------------------------------------------------------------------
create table if not exists public.audit_logs (
  id          uuid primary key default gen_random_uuid(),
  actor_id    uuid references public.profiles(id),
  action      text not null,
  entity      text not null,
  entity_id   uuid,
  metadata    jsonb not null default '{}'::jsonb,
  created_at  timestamptz not null default now()
);

create index if not exists audit_logs_created_idx on public.audit_logs (created_at desc);

-- ---------------------------------------------------------------------------
-- integration_outbox — reliable delivery for n8n / WhatsApp / Sheets
-- ---------------------------------------------------------------------------
create table if not exists public.integration_outbox (
  id          uuid primary key default gen_random_uuid(),
  kind        text not null,
  payload     jsonb not null default '{}'::jsonb,
  status      text not null default 'PENDING'
              check (status in ('PENDING', 'SENT', 'FAILED')),
  attempts    integer not null default 0,
  created_at  timestamptz not null default now(),
  sent_at     timestamptz
);

create index if not exists integration_outbox_status_idx on public.integration_outbox (status, created_at);

-- ===========================================================================
-- Authorization helpers
-- ===========================================================================
create or replace function public.current_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select role from public.profiles where id = auth.uid();
$$;

create or replace function public.is_owner()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (select 1 from public.profiles where id = auth.uid() and role = 'OWNER');
$$;

create or replace function public.is_staff()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid() and role in ('OWNER', 'STAFF')
  );
$$;

-- ===========================================================================
-- Row Level Security
-- ===========================================================================
alter table public.profiles            enable row level security;
alter table public.members             enable row level security;
alter table public.membership_plans    enable row level security;
alter table public.memberships         enable row level security;
alter table public.payments            enable row level security;
alter table public.payment_events      enable row level security;
alter table public.notifications       enable row level security;
alter table public.gym_settings        enable row level security;
alter table public.staff_permissions   enable row level security;
alter table public.audit_logs          enable row level security;
alter table public.integration_outbox  enable row level security;

-- profiles
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
for select using (id = auth.uid() or public.is_staff());

drop policy if exists profiles_update on public.profiles;
create policy profiles_update on public.profiles
for update using (id = auth.uid() or public.is_owner()) with check (true);

-- members: staff manage all; members read their own row (member id == auth uid)
drop policy if exists members_all_staff on public.members;
create policy members_all_staff on public.members
for all using (public.is_staff()) with check (public.is_staff());

drop policy if exists members_select_self on public.members;
create policy members_select_self on public.members
for select using (id = auth.uid());

-- membership_plans
drop policy if exists plans_select on public.membership_plans;
create policy plans_select on public.membership_plans
for select using (auth.uid() is not null);

drop policy if exists plans_write on public.membership_plans;
create policy plans_write on public.membership_plans
for all using (public.is_owner()) with check (public.is_owner());

-- memberships
drop policy if exists memberships_all_staff on public.memberships;
create policy memberships_all_staff on public.memberships
for all using (public.is_staff()) with check (public.is_staff());

drop policy if exists memberships_select_self on public.memberships;
create policy memberships_select_self on public.memberships
for select using (member_id = auth.uid());

-- payments
drop policy if exists payments_all_staff on public.payments;
create policy payments_all_staff on public.payments
for all using (public.is_staff()) with check (public.is_staff());

drop policy if exists payments_select_self on public.payments;
create policy payments_select_self on public.payments
for select using (member_id = auth.uid());

-- payment_events: readable by staff; writes happen inside RPCs only
drop policy if exists payment_events_staff on public.payment_events;
create policy payment_events_staff on public.payment_events
for select using (public.is_staff());

-- notifications
drop policy if exists notifications_read on public.notifications;
create policy notifications_read on public.notifications
for select using (auth.uid() is not null);

-- gym_settings
drop policy if exists gym_settings_read on public.gym_settings;
create policy gym_settings_read on public.gym_settings
for select using (auth.uid() is not null);

drop policy if exists gym_settings_write on public.gym_settings;
create policy gym_settings_write on public.gym_settings
for all using (public.is_owner()) with check (public.is_owner());

-- staff_permissions
drop policy if exists staff_permissions_read on public.staff_permissions;
create policy staff_permissions_read on public.staff_permissions
for select using (public.is_owner() or staff_id = auth.uid());

drop policy if exists staff_permissions_write on public.staff_permissions;
create policy staff_permissions_write on public.staff_permissions
for all using (public.is_owner()) with check (public.is_owner());

-- audit_logs
drop policy if exists audit_logs_read on public.audit_logs;
create policy audit_logs_read on public.audit_logs
for select using (public.is_owner());

-- integration_outbox is server-only (service role)
drop policy if exists integration_outbox_read on public.integration_outbox;
create policy integration_outbox_read on public.integration_outbox
for select using (public.is_owner());

-- ===========================================================================
-- View: member_overview (member + current membership + outstanding due)
--
-- security_invoker = true is REQUIRED: without it the view runs with the
-- owner's privileges and silently bypasses RLS, which would let any
-- authenticated member read every member's phone, email and dues.
-- ===========================================================================
create or replace view public.member_overview
with (security_invoker = true) as
with ranked as (
  select m.*,
         row_number() over (partition by m.member_id order by m.end_date desc, m.created_at desc) as rn
  from public.memberships m
),
due_calc as (
  select p.membership_id,
         sum(case when p.status = 'SUCCESS' then p.amount else 0 end) as paid,
         sum(case when p.status = 'REFUNDED' then p.amount else 0 end) as refunded
  from public.payments p
  group by p.membership_id
)
select
  m.id as member_id,
  jsonb_build_object(
    'id', m.id,
    'full_name', m.full_name,
    'phone', m.phone,
    'email', m.email,
    'status', m.status,
    'joined_at', m.joined_at,
    'photo_url', m.photo_url,
    'created_at', m.created_at
  ) as member,
  case when r.id is null then null else jsonb_build_object(
    'id', r.id,
    'member_id', r.member_id,
    'plan_id', r.plan_id,
    'plan_name', r.plan_name,
    'start_date', r.start_date,
    'end_date', r.end_date,
    'fee', r.fee,
    'status', r.status,
    'created_at', r.created_at
  ) end as current_membership,
  coalesce(
    greatest(r.fee - coalesce(d.paid, 0) + coalesce(d.refunded, 0), 0),
    0
  ) as due_amount
from public.members m
left join ranked r on r.member_id = m.id and r.rn = 1
left join due_calc d on d.membership_id = r.id;

-- The view is security_invoker, so the caller's RLS policies apply.
-- Expose it to authenticated users.
grant select on public.member_overview to authenticated;

-- ===========================================================================
-- RPC: create_membership — server-authoritative fee and dates
-- ===========================================================================
create or replace function public.create_membership(
  p_member_id uuid,
  p_plan_id uuid,
  p_start_date date,
  p_start_after_current boolean default true
)
returns public.memberships
language plpgsql
security definer
set search_path = public
as $$
declare
  v_plan        public.membership_plans;
  v_last_end    date;
  v_start       date;
  v_end         date;
  v_row         public.memberships;
begin
  if not public.is_staff() then
    raise exception 'Not authorized' using errcode = '42501';
  end if;

  select * into v_plan from public.membership_plans where id = p_plan_id;
  if v_plan.id is null then
    raise exception 'Plan not found' using errcode = 'P0002';
  end if;

  select max(end_date) into v_last_end
  from public.memberships
  where member_id = p_member_id and end_date >= current_date;

  if p_start_after_current and v_last_end is not null then
    v_start := v_last_end + 1;
  else
    v_start := greatest(p_start_date, current_date);
  end if;

  v_end := (v_start + (v_plan.duration_months || ' months')::interval)::date - 1;

  insert into public.memberships (member_id, plan_id, plan_name, start_date, end_date, fee, status)
  values (p_member_id, v_plan.id, v_plan.name, v_start, v_end, v_plan.fee, 'ACTIVE')
  returning * into v_row;

  insert into public.audit_logs (actor_id, action, entity, entity_id, metadata)
  values (auth.uid(), 'CREATE_MEMBERSHIP', 'memberships', v_row.id,
          jsonb_build_object('member_id', p_member_id, 'plan_id', p_plan_id));

  return v_row;
end;
$$;

-- ===========================================================================
-- RPC: record_payment — validates due, enforces idempotency
-- ===========================================================================
create or replace function public.record_payment(
  p_member_id uuid,
  p_membership_id uuid,
  p_amount numeric,
  p_method text,
  p_reference text,
  p_note text,
  p_idempotency_key text,
  p_collected_by uuid
)
returns public.payments
language plpgsql
security definer
set search_path = public
as $$
declare
  v_existing    public.payments;
  v_membership  public.memberships;
  v_due         numeric(12, 2);
  v_paid        numeric(12, 2);
  v_status      text;
  v_row         public.payments;
begin
  if not public.is_staff() then
    raise exception 'Not authorized' using errcode = '42501';
  end if;

  -- Idempotency: return the original row if the key was already used.
  select * into v_existing from public.payments where idempotency_key = p_idempotency_key;
  if v_existing.id is not null then
    return v_existing;
  end if;

  if p_amount is null or p_amount <= 0 then
    raise exception 'Amount must be greater than zero' using errcode = 'P0001';
  end if;

  -- Resolve the membership (explicit or latest).
  if p_membership_id is not null then
    select * into v_membership from public.memberships where id = p_membership_id;
  else
    select * into v_membership
    from public.memberships
    where member_id = p_member_id
    order by end_date desc
    limit 1;
  end if;

  if v_membership.id is not null then
    select coalesce(sum(amount), 0) into v_paid
    from public.payments
    where membership_id = v_membership.id and status = 'SUCCESS';

    v_due := greatest(v_membership.fee - v_paid, 0);

    if p_amount > v_due then
      raise exception 'Amount exceeds outstanding due' using errcode = 'P0001';
    end if;
  end if;

  -- Cash is immediately successful; online/mobile money stays pending until verified.
  v_status := case when p_method = 'CASH' then 'SUCCESS' else 'PENDING' end;

  insert into public.payments (
    member_id, membership_id, amount, method, status, paid_at, reference, note,
    collected_by, idempotency_key
  )
  values (
    p_member_id, v_membership.id, p_amount, p_method, v_status,
    case when v_status = 'SUCCESS' then now() else null end,
    p_reference, p_note, p_collected_by, p_idempotency_key
  )
  returning * into v_row;

  insert into public.payment_events (payment_id, type, amount)
  values (v_row.id, 'CREATED', p_amount);

  if v_status = 'SUCCESS' then
    insert into public.payment_events (payment_id, type, amount)
    values (v_row.id, 'VERIFIED', p_amount);
  end if;

  insert into public.audit_logs (actor_id, action, entity, entity_id, metadata)
  values (auth.uid(), 'RECORD_PAYMENT', 'payments', v_row.id,
          jsonb_build_object('member_id', p_member_id, 'amount', p_amount, 'method', p_method));

  -- Queue a WhatsApp receipt via the outbox (delivered by n8n).
  insert into public.integration_outbox (kind, payload)
  values ('payment_receipt', jsonb_build_object('payment_id', v_row.id, 'member_id', p_member_id));

  return v_row;
end;
$$;

grant execute on function public.create_membership(uuid, uuid, date, boolean) to authenticated;
grant execute on function public.record_payment(uuid, uuid, numeric, text, text, text, text, uuid) to authenticated;

-- ===========================================================================
-- Seed default plans
-- ===========================================================================
insert into public.membership_plans (name, duration_months, fee, description)
select * from (values
  ('Monthly',  1,  1500.00, 'Flexible month-to-month access'),
  ('Quarterly', 3, 4000.00, 'Save with a 3-month plan'),
  ('Half-Yearly', 6, 7500.00, 'Best for regular members'),
  ('Yearly',  12, 14000.00, 'Best value for committed members')
) as seed(name, duration_months, fee, description)
where not exists (select 1 from public.membership_plans);