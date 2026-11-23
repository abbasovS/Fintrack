# FinTrack Web

React + TypeScript frontend for the FinTrack Spring Boot API. The dashboard, transaction list, category manager, subscription planner, and reports read real data from PostgreSQL through the API; the UI does not generate sample amounts or transactions.

## Run locally

1. Start PostgreSQL, Vault, Consul, and the local Mailpit inbox with `docker compose up -d` from the repository root.
2. Start the Spring Boot API on port `8081` with the project's environment configuration.
3. From this directory, run `npm install` and then `npm run dev`.
4. Open `http://localhost:5173`.

For local email verification with Gmail, copy the SMTP settings from `.env.example` into the ignored root `.env` file and replace the example address and password. The registration creates an account only after the emailed code is confirmed. The code expires after 10 minutes; requesting another code is limited to once per minute. To capture mail in Mailpit instead, set `SMTP_HOST=localhost`, `SMTP_PORT=1025`, `SMTP_AUTH=false`, `SMTP_STARTTLS=false`, and `MAIL_FROM=no-reply@fintrack.local`.

For a deployed environment, configure `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS`, and `MAIL_FROM` with the email provider's SMTP settings. Keep SMTP credentials in the deployment secret store, not in source control.

Vite proxies `/api` to `http://localhost:8081`, avoiding a separate browser CORS setup during local development.

## API features used

- `GET /api/v1/users/me` loads the authenticated profile.
- `/api/v1/categories` lists and manages the authenticated user's categories. Legacy categories without an owner remain visible and read-only.
- `/api/v1/transactions` creates, edits, removes, filters, and paginates the authenticated user's transactions.
- `/api/v1/reports/summary`, `/api/v1/reports/categories`, and `/api/v1/reports/trend` load real totals and trends for the selected period.
- `/api/v1/subscriptions` manages recurring-payment plans and their generated billing cycles. Confirming a cycle creates one linked expense transaction; skipping a cycle creates no transaction. The monthly available-cash estimate subtracts unpaid subscription cycles from this month's actual income after actual expenses.
- `/api/v1/auth/register`, `/verify-email`, and `/resend-verification` run the email OTP registration flow; login is available only after verification.
- `/api/v1/users/me` updates the profile name, while `/api/v1/users/me/password` changes the password after checking the current password.

The app currently assumes one currency, AZN. The API returns the configured currency (`FINTRACK_CURRENCY`, default `AZN`).

## Structure

- `src/app` — routes and application composition
- `src/components` — shared layout and route guards
- `src/features` — auth, dashboard, transactions, categories, and subscriptions
- `src/lib/api` — typed API client and response/request types
- `src/lib/auth` — JWT session helpers
- `src/styles` — global and responsive styles

The JWT is held in `sessionStorage` because the current API returns it in the login response. For production, an `HttpOnly`, `Secure`, `SameSite` cookie with CSRF protection is preferable.
