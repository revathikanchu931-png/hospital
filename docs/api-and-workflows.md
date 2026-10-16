# API and workflow notes

The backend uses Spring MVC controllers, service methods, Spring Data repositories, Hibernate and Flyway. Registration, login, email verification, password reset and approved public provider/care listings are public. Account state, role and ownership checks are enforced by Spring Security and service logic. Application data belongs to MySQL.

## Implemented endpoints

- `POST /api/auth/register` and `/api/auth/register/patient` create a patient account. `/api/auth/register/doctor`, `/pharmacy`, and `/laboratory` accept their respective role-specific fields and create email-unverified, approval-pending provider accounts.
- `POST /api/auth/register/admin` (alias `/api/auth/setup-admin`) provisions the first administrator only when the `X-Setup-Token` matches `ADMIN_SETUP_TOKEN`. Once an admin exists, the route is closed. The organization name is required.
- `POST /api/auth/verify-email` consumes the one-time email token and returns the resulting account status. Patient and authorized initial-admin accounts activate after verification; doctors, pharmacies and laboratories remain pending administrator approval.
- `POST /api/auth/resend-verification` accepts `{email, role}` and returns a neutral response. For an eligible unverified account it rotates the token and sends a new link; the account has a persistent 60-second resend cooldown.
- `POST /api/auth/forgot-password` accepts `{email, role}` and always returns a neutral response to avoid account enumeration. `POST /api/auth/reset-password` consumes a single-use token, hashes the new password with BCrypt, and does not change role, email verification or approval status. Reset requests have a persistent 60-second per-account cooldown.
- Spring Mail uses `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_SMTP_SSL`, and `MAIL_FROM`. Local development can use the repository's Mailpit compose service. Registration responses include `verificationEmailSent`; failed delivery leaves the account saved but unverified so the user can retry through the resend endpoint. Raw tokens are stored only as SHA-256 hashes and never returned by the API or logged.
- `GET /api/auth/csrf` issues a CSRF token. `POST /api/auth/login`, `POST /api/auth/logout`, and `GET /api/auth/me` use CSRF-protected Spring Security sessions and a database-backed `UserDetailsService`. Login requires the requested `role` and rejects a mismatch with a generic credential error.
- `GET /api/profile/me` and `PUT /api/profile/me` let the signed-in user view their account/profile data and update common name/phone fields.
- `GET /api/doctors`, `GET /api/doctors/{id}`, `GET /api/doctors/{id}/availability` show only active, approved provider accounts; approved doctors can add future availability at `POST /api/doctors/availability`.
- `POST /api/appointments`, `GET /api/appointments/my`, `PUT /api/appointments/{id}/reschedule`, `PATCH /api/appointments/{id}/cancel`, and `PATCH /api/appointments/{id}/status` enforce patient/doctor record ownership. Database uniqueness constraints protect appointment slots.
- `GET /api/labs` lists approved laboratories; test catalog currently returns an empty list.
- Admin users can read dashboard counts, all account summaries and pending providers; approve/reject provider accounts; create doctor, pharmacy and laboratory staff accounts; and activate/deactivate accounts. Sensitive account changes are recorded in `account_audit_events`.

The V1 Flyway schema also lays out diagnostic booking, report, pharmacy, cart, prescription, order, provider verification and audit tables. Their APIs and workflows are not yet implemented. V4 adds account status, email verification, organization name and hashed action-token storage; V5 adds nullable email-send timestamps for persistent resend/reset cooldowns. These are incremental changes and do not drop existing records. The admin setup route is a backend bootstrap tool guarded by a private setup token, not an unrestricted role-assignment API.

## Local API requests

Initial admin setup (send once, replace the token, and do not put it in frontend code):

```powershell
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = (Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/auth/csrf -WebSession $session).token
$body = @{ name = 'MediConnect Administrator'; email = 'admin@example.test'; phone = '9000000000'; organizationName = 'MediConnect Demo'; password = 'ChangeThisToALongUniquePassword'; confirmPassword = 'ChangeThisToALongUniquePassword' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/setup-admin -WebSession $session -Headers @{ 'X-Setup-Token' = $env:ADMIN_SETUP_TOKEN; 'X-XSRF-TOKEN' = $csrf } -ContentType 'application/json' -Body $body
```

Subsequent mutating API calls pass the same `-WebSession $session` and current `X-XSRF-TOKEN`; browser requests handle cookie/header exchange automatically. Browser sessions use servlet cookies.

All registration requests need matching `password` and `confirmPassword` values. Provider request field names are available in `backend/src/main/java/com/mediconnect/dto/AuthDtos.java`. Configure Mailpit or SMTP so registration can deliver the verification message; the frontend never receives the raw verification or password-reset token except through the configured mail link.
