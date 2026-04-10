CREATE TYPE lease_status AS ENUM (
    'DRAFT', 'AWAITING_LANDLORD_SIGNATURE', 'AWAITING_TENANT_SIGNATURE',
    'FULLY_SIGNED', 'TAX_REGISTERED', 'TERMINATED'
);

CREATE TABLE lease_contract (
    id                    BIGSERIAL      PRIMARY KEY,
    booking_id            BIGINT         NOT NULL UNIQUE REFERENCES booking(id),
    start_date            DATE           NOT NULL,
    end_date              DATE           NOT NULL,
    monthly_rent          DECIMAL(19,2)  NOT NULL,
    security_deposit      DECIMAL(19,2)  NOT NULL,
    agency_fee            DECIMAL(19,2)  NOT NULL,
    special_clauses       TEXT           NULL,
    pdf_url               VARCHAR(500)   NULL,
    status                lease_status   NOT NULL DEFAULT 'DRAFT',
    landlord_signed_at    TIMESTAMP      NULL,
    tenant_signed_at      TIMESTAMP      NULL,
    tax_registration_date DATE           NULL,
    tax_reference         VARCHAR(100)   NULL,
    generated_by_id       BIGINT         NOT NULL REFERENCES users(id),
    created_at            TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lease_contract_booking_id ON lease_contract(booking_id);
CREATE INDEX idx_lease_contract_status ON lease_contract(status);
