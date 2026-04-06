CREATE TYPE landlord_document_type AS ENUM ('ID_CARD', 'BUILDING_PERMIT', 'LAND_TITLE', 'OTHER');
CREATE TYPE document_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');

CREATE TABLE landlord_document (
    id                BIGSERIAL        PRIMARY KEY,
    landlord_id       BIGINT           NOT NULL REFERENCES users(id),
    type              landlord_document_type NOT NULL,
    file_name         VARCHAR(255)     NOT NULL,
    storage_url       VARCHAR(500)     NOT NULL,
    created_at        TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP        NOT NULL DEFAULT NOW(),
    status            document_status  NOT NULL DEFAULT 'PENDING',
    validated_by_id   BIGINT           NULL REFERENCES users(id),
    validated_at      TIMESTAMP        NULL,
    rejection_comment TEXT             NULL
);

CREATE INDEX idx_landlord_document_landlord_id ON landlord_document(landlord_id);
CREATE INDEX idx_landlord_document_status ON landlord_document(status);
