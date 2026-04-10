CREATE TYPE application_status AS ENUM ('INCOMPLETE', 'SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'REJECTED');
CREATE TYPE tenant_document_type AS ENUM ('ID_CARD', 'STUDENT_CARD', 'PROOF_OF_ADDRESS', 'GUARANTOR_ID_CARD', 'OTHER');

CREATE TABLE tenant_application (
    id               BIGSERIAL          PRIMARY KEY,
    booking_id       BIGINT             NOT NULL UNIQUE REFERENCES booking(id),
    status           application_status NOT NULL DEFAULT 'INCOMPLETE',
    created_at       TIMESTAMP          NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP          NOT NULL DEFAULT NOW(),
    decided_at       TIMESTAMP          NULL,
    decision_comment TEXT               NULL,
    decided_by_id    BIGINT             NULL REFERENCES users(id)
);

CREATE INDEX idx_tenant_application_booking_id ON tenant_application(booking_id);
CREATE INDEX idx_tenant_application_status ON tenant_application(status);

CREATE TABLE tenant_document (
    id              BIGSERIAL            PRIMARY KEY,
    application_id  BIGINT               NOT NULL REFERENCES tenant_application(id),
    type            tenant_document_type NOT NULL,
    file_name       VARCHAR(255)         NOT NULL,
    storage_url     VARCHAR(500)         NOT NULL,
    created_at      TIMESTAMP            NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP            NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_document_application_id ON tenant_document(application_id);

CREATE TABLE guarantor (
    id                       BIGSERIAL    PRIMARY KEY,
    application_id           BIGINT       NOT NULL REFERENCES tenant_application(id),
    last_name                VARCHAR(100) NOT NULL,
    first_name               VARCHAR(100) NOT NULL,
    id_card_number           VARCHAR(50)  NOT NULL,
    phone                    VARCHAR(20)  NOT NULL,
    address                  VARCHAR(500) NOT NULL,
    relationship_to_tenant   VARCHAR(100) NOT NULL,
    created_at               TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_guarantor_application_id ON guarantor(application_id);
