CREATE TABLE PROGRAM
(
    id                UUID PRIMARY KEY,
    short_name        VARCHAR(255) UNIQUE NOT NULL,
    name              VARCHAR(255) UNIQUE NOT NULL,
    description       VARCHAR(255)        NOT NULL DEFAULT '',
    membership_type   VARCHAR(255)        NOT NULL,
    commitment_donors INT                 NOT NULL,
    submitted_donors  INT                 NOT NULL,
    genomic_donors    INT                 NOT NULL,
    website           VARCHAR(255)        NOT NULL,
    date_created      TIMESTAMP           NOT NULL
);
