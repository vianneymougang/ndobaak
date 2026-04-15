CREATE TYPE inspection_type AS ENUM ('MOVE_IN', 'MOVE_OUT');
CREATE TYPE inspection_status AS ENUM ('IN_PROGRESS', 'COMPLETED', 'LANDLORD_SIGNED', 'FULLY_SIGNED');
CREATE TYPE element_condition AS ENUM ('GOOD', 'NORMAL_WEAR', 'DAMAGED', 'MISSING');

CREATE TABLE property_inspection (
    id                 BIGSERIAL          PRIMARY KEY,
    booking_id         BIGINT             NOT NULL REFERENCES booking(id),
    type               inspection_type    NOT NULL,
    conducted_at       TIMESTAMP          NOT NULL,
    agent_id           BIGINT             NOT NULL REFERENCES users(id),
    status             inspection_status  NOT NULL DEFAULT 'IN_PROGRESS',
    landlord_signed    BOOLEAN            NOT NULL DEFAULT FALSE,
    tenant_signed      BOOLEAN            NOT NULL DEFAULT FALSE,
    landlord_signed_at TIMESTAMP          NULL,
    tenant_signed_at   TIMESTAMP          NULL,
    observations       TEXT               NULL,
    created_at         TIMESTAMP          NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP          NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_property_inspection_booking_id ON property_inspection(booking_id);

CREATE TABLE inspection_item (
    id             BIGSERIAL         PRIMARY KEY,
    inspection_id  BIGINT            NOT NULL REFERENCES property_inspection(id),
    location       VARCHAR(100)      NOT NULL,
    element        VARCHAR(100)      NOT NULL,
    condition      element_condition NOT NULL,
    observations   TEXT              NULL,
    created_at     TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inspection_item_inspection_id ON inspection_item(inspection_id);

CREATE TABLE inspection_photo (
    id             BIGSERIAL    PRIMARY KEY,
    inspection_id  BIGINT       NOT NULL REFERENCES property_inspection(id),
    item_id        BIGINT       NULL REFERENCES inspection_item(id),
    storage_url    VARCHAR(500) NOT NULL,
    caption        VARCHAR(255) NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inspection_photo_inspection_id ON inspection_photo(inspection_id);
CREATE INDEX idx_inspection_photo_item_id ON inspection_photo(item_id);
