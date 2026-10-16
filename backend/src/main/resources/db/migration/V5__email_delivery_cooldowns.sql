ALTER TABLE users
  ADD COLUMN verification_email_sent_at TIMESTAMP(6) NULL,
  ADD COLUMN password_reset_email_sent_at TIMESTAMP(6) NULL;
