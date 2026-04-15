CREATE TABLE key_handover (
    id                      BIGSERIAL  PRIMARY KEY,
    booking_id              BIGINT     NOT NULL UNIQUE REFERENCES booking(id),
    move_in_inspection_id   BIGINT     NOT NULL REFERENCES property_inspection(id),
    handover_date           TIMESTAMP  NOT NULL,
    landlord_confirmed      BOOLEAN    NOT NULL DEFAULT FALSE,
    tenant_confirmed        BOOLEAN    NOT NULL DEFAULT FALSE,
    landlord_confirmed_at   TIMESTAMP  NULL,
    tenant_confirmed_at     TIMESTAMP  NULL,
    transfer_initiated      BOOLEAN    NOT NULL DEFAULT FALSE,
    transfer_date           TIMESTAMP  NULL,
    observations            TEXT       NULL,
    created_at              TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_key_handover_booking_id ON key_handover(booking_id);
