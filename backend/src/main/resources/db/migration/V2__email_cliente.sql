-- E-mail opcional do cliente (confirmação/lembrete) e controle de lembrete já enviado.
ALTER TABLE appointment
    ADD COLUMN customer_email   VARCHAR(160),
    ADD COLUMN reminder_sent_at TIMESTAMPTZ;

CREATE INDEX idx_appointment_reminder ON appointment (starts_at)
    WHERE status = 'CONFIRMED' AND customer_email IS NOT NULL AND reminder_sent_at IS NULL;
