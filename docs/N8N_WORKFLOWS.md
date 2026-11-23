# n8n automation workflows

n8n is the automation layer. It never runs in the app; it consumes Supabase data
and sends WhatsApp messages / syncs Google Sheets using server-side credentials.

## Reliable outbox pattern

Financial and notification side effects are written to `integration_outbox` inside
the same transaction as the business change. n8n polls this table, delivers the
message, and marks the row `SENT` (or `FAILED` with an attempt count). This avoids
lost notifications and duplicate sends.

```
Android  ->  Supabase RPC  ->  integration_outbox (PENDING)
[n8n cron] -> poll PENDING -> send via provider -> mark SENT/FAILED
```

## 1) Expiring membership reminders (daily)

- **Trigger:** Schedule (daily 09:00 Asia/Dhaka).
- **Query:** members whose current membership ends within 3 days.
  ```sql
  select m.phone, m.full_name, me.end_date
  from public.members m
  join public.memberships me on me.member_id = m.id
  where me.end_date between current_date and current_date + 3
    and not exists (
      select 1 from public.memberships newer
      where newer.member_id = m.id and newer.start_date > me.end_date
    );
  ```
- **Action:** send a WhatsApp template ("Your membership expires on {{end_date}}").
- **Log:** insert into `notifications` and mark any outbox rows `SENT`.

Sample workflow: [`workflows/expiring_reminders.json`](../workflows/expiring_reminders.json)

## 2) Payment receipt via WhatsApp

- **Trigger:** Poll `integration_outbox` where `kind = 'payment_receipt'`.
- **Steps:**
  1. Read `payload.payment_id`.
  2. Fetch payment + member phone from Supabase.
  3. Send WhatsApp receipt template with amount, method, date.
  4. Update the outbox row to `SENT`.
- **Idempotency:** the outbox row is unique per payment; mark `SENT` after success.

Sample workflow: [`workflows/payment_receipt_whatsapp.json`](../workflows/payment_receipt_whatsapp.json)

## 3) Refund receipt via WhatsApp

- **Trigger:** Poll `integration_outbox` where `kind = 'refund_receipt'` (written by
  the `refund_payment` RPC).
- **Steps:**
  1. Read `payload.payment_id`.
  2. Fetch the refund row + member phone.
  3. Send the `refund_receipt` template.
  4. Mark the outbox row `SENT`.

Sample workflow: [`workflows/refund_receipt_whatsapp.json`](../workflows/refund_receipt_whatsapp.json)

## 4) Daily report to Google Sheets

- **Trigger:** Schedule (daily 22:30 Asia/Dhaka) or owner taps "Export".
- **Steps:**
  1. Compute the daily report (same rules as `DailyReport`).
  2. Append a row to a Google Sheet (Date, Collection, Cash, Online, New, Renewals, Dues).
  3. Insert a `notifications` row so the owner sees the summary in-app.
- **Setup:**
  1. Create a spreadsheet with a header row: `date, collection, cash, online, new_members, renewals, payments_count, dues`.
  2. Share the sheet with the n8n Google service account (Editor).
  3. Open [`workflows/daily_report_to_sheets.json`](../workflows/daily_report_to_sheets.json) in n8n, replace
     `YOUR_GOOGLE_SHEET_ID`, and pick the sheet tab.
  4. Add a Google credential in n8n (service account JSON with Sheets scope).
- **Idempotency:** the run is date-scoped; re-running for the same business date appends a
  second row. Add a unique `date` check in the sheet if you need exactly-once rows.

Sample workflow: [`workflows/daily_report_to_sheets.json`](../workflows/daily_report_to_sheets.json)

## Credentials (n8n, never in the app)

- Supabase: service-role key + project URL.
- WhatsApp Business Platform: access token + phone number ID.
- Google: service account with Sheets scope.

## Failure handling

- Retry with exponential backoff (e.g., 3 attempts).
- On final failure set `status = 'FAILED'` and increment `attempts`.
- Alert the owner via a `GENERAL` notification.