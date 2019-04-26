CREATE TABLE program
(
    id                UUID PRIMARY KEY,
    short_name        VARCHAR(255) UNIQUE NOT NULL,
    name              VARCHAR(255) UNIQUE NOT NULL,
    description       VARCHAR(255)        NOT NULL DEFAULT '',
    membership_type   VARCHAR(32)         NOT NULL,
    commitment_donors INT                 NOT NULL DEFAULT 0,
    submitted_donors  INT                 NOT NULL DEFAULT 0,
    genomic_donors    INT                 NOT NULL DEFAULT 0,
    website           VARCHAR(255)        NOT NULL,
    created_at        DATE                NOT NULL,
    updated_at        DATE,
    institutions      varchar(255)        DEFAULT '',
    countries         varchar(255)        DEFAULT '',
    regions           varchar(255)        DEFAULT ''
);
