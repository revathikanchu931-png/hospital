# MediConnect

MediConnect is a React and Spring Boot healthcare platform foundation. It includes a responsive public site, self-registration and sign-in flows for patient, doctor, admin, pharmacy and laboratory accounts, database-backed session authentication, email verification and password reset, initial-admin setup, provider approval, appointment workflows, role-based dashboard navigation, and MySQL metrics. MySQL is the only application persistence store.

> **Implementation status:** This is a working foundation, not the complete platform described in the original product brief. Diagnostic booking/reporting, medicine inventory/cart/prescription/order workflows, and production email deliverability monitoring remain to be implemented. SMTP must be configured to deliver verification and password-reset links.

## Project structure

```text
frontend/                     React 18, Vite, Bootstrap, Router, Axios, Lucide
backend/                      Java 21, Spring Boot 3, Security, JPA, MySQL, Flyway
  src/main/resources/db/migration/  MySQL schema migrations
database/create-database.sql  Local database and least-scope user setup
docs/api-and-workflows.md     API behavior and bootstrap notes
.env.example                  Local environment-variable template
```

## Prerequisites

- Windows 11, Java 21, MySQL 8, Node.js LTS and npm.
- The backend includes a Maven Wrapper (`mvnw.cmd`); it downloads the configured Maven distribution on first use if needed.

## Configure MySQL

Open a PowerShell window in the project root. Edit the sample database-user password before applying the script if desired.

```powershell
mysql -u root -p < .\database\create-database.sql
```

Flyway creates application tables on first startup. `ddl-auto: validate` checks that Hibernate mappings agree with the migration schema. The backend uses `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`; on PowerShell, set them in the same terminal that starts Spring Boot:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/mediconnect_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME = 'mediconnect'
$env:DB_PASSWORD = 'change-me'
$env:APP_CORS_ORIGIN = 'http://localhost:5173'
$env:ADMIN_SETUP_TOKEN = 'replace-with-a-long-random-secret'
$env:APP_PUBLIC_BASE_URL = 'http://localhost:5173'
$env:MAIL_HOST = 'localhost'
$env:MAIL_PORT = '1025'
$env:MAIL_SMTP_AUTH = 'false'
$env:MAIL_SMTP_STARTTLS = 'false'
$env:MAIL_SMTP_SSL = 'false'
$env:MAIL_FROM = 'no-reply@mediconnect.local'
```

Replace every sample password/secret with a private value. `.env.example` is a reference; Spring does not automatically load a `.env` file. For local mail testing, leave `MAIL_HOST=localhost`, `MAIL_PORT=1025`, SMTP auth and TLS disabled, and start the Mailpit service below.

## Start the backend

```powershell
cd .\backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

The API listens on `http://localhost:8080`; Flyway migrations run at startup. A configured, reachable MySQL 8 instance is required. To run unit/integration tests once Maven dependencies are available:

```powershell
.\mvnw.cmd test
```

## Create accounts and verify email

For local testing, start the development email sink before running the backend:

```powershell
docker compose -f .\docker-compose.mailpit.yml up -d
```

Mailpit accepts SMTP on `localhost:1025` and displays captured messages at `http://localhost:8025`. Set the backend `MAIL_HOST=localhost`, `MAIL_PORT=1025`, `MAIL_SMTP_AUTH=false`, `MAIL_SMTP_STARTTLS=false`, and `MAIL_SMTP_SSL=false`. Open the verification/reset link in the captured message. The application never prints raw one-time links to the backend logs. If Mailpit is unavailable, account registration still saves the account but reports that no verification email was delivered; use the resend form after email delivery is restored. Resend and password reset have a 60-second account cooldown.

For real delivery, configure Spring Mail through environment variables. For Gmail, use `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_USERNAME` set to the sending Gmail address, `MAIL_PASSWORD` set to a Google App Password (not the account password), `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`, `MAIL_SMTP_SSL=false`, and `MAIL_FROM` set to that same sender address. Availability of Gmail App Passwords depends on the Google account's security setup. Never put SMTP secrets in Vite variables or frontend files.

Patients, doctors, pharmacies, laboratories, and the authorized initial administrator can register through their respective forms. The server hashes passwords and stores account/profile data in MySQL. Patients become active after email verification. Verified doctor, pharmacy, and laboratory accounts remain blocked from sign-in until an administrator approves them. Forgot-password requests are neutral and do not disclose whether an account exists; the reset email is sent only to email-verified, eligible accounts. Admin registration at `/register/admin` is protected by `ADMIN_SETUP_TOKEN` and allowed only while no administrator exists. Enter that secret in the form; it is sent only to the backend in an HTTPS header and is never bundled into frontend source. Rotate or remove the initial setup token after use.

The five sign-in routes are `/login/patient`, `/login/doctor`, `/login/admin`, `/login/pharmacy`, and `/login/laboratory`; `/login` presents the role chooser. Sessions use Spring Security, BCrypt, HTTP-only session cookies, and CSRF tokens. User role is determined from MySQL and must match the requested login role.

## Start the frontend

In another PowerShell window:

```powershell
cd .\frontend
npm install
npm run dev
```

Open `http://localhost:5173`. To build production frontend assets, run `npm run build`; to run frontend tests, run `npm test` (test scripts are configured, but application test cases have not yet been written).

The UI is responsive and routes patient/doctor/laboratory/pharmacy/admin users to separate dashboard layouts. Profile read/update, logout, provider approval and account activation are backed by protected APIs.

## Security and operational notes

- Passwords use BCrypt; role-specific registration endpoints assign roles server-side. The first admin is provisioned using a high-entropy setup token. Email verification and password reset tokens are random, stored as SHA-256 hashes, expire, are single-use, and prior outstanding tokens are invalidated on resend. Resend/reset requests are serialized per account and rate-limited with a 60-second cooldown.
- API responses do not include password hashes. Admin provider and user routes are role-protected; appointment workflows enforce record ownership.
- Before internet-facing deployment, configure HTTPS/secure cookie settings, review cookie SameSite policy, add IP-level abuse controls, use non-default database secrets, and verify the production SMTP provider and sender domain.
- Avoid real patient data during local development. All example identity strings are fictional.

## Current limitations

- Self-registration captures patient and provider profile details. Profile editing currently covers common name and phone fields; role-specific profile editing is limited.
- Diagnostic test booking, report uploads/downloads, medicine listing from MySQL, cart, prescription upload/review and order fulfillment endpoints are not yet implemented. Corresponding MySQL tables are provisioned for that work.
- Email delivery uses Spring Mail. A local Mailpit SMTP sink is provided for development; production email delivery still requires valid SMTP credentials and a verified sender. When SMTP delivery fails, registration remains saved, the API reports that the verification message was not sent, and the user can request a resend later. Forgot-password responses remain neutral to avoid account enumeration.
- Local verification requires Docker/Mailpit or an SMTP provider. This workspace's tests do not prove delivery through an external production mail provider.
- The migration includes table layouts for the broader requested data model. A deployment team should review schema policy, consent and audit requirements before production use.
