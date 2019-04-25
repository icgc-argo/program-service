CREATE TYPE membership AS ENUM ('FULL', 'ASSOCIATE');

CREATE TABLE cancer (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE primary_site  (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE program_cancer (
  program_id          UUID NOT NULL,
  cancer_id           UUID NOT NULL,

  PRIMARY KEY(program_id, cancer_id),
  FOREIGN KEY(program_id) REFERENCES program(id),
  FOREIGN KEY(cancer_id)  REFERENCES cancer(id)
);

CREATE TABLE program_primary_site (
  program_id            UUID  NOT NULL,
  primary_site_id               UUID  NOT NULL,

  PRIMARY KEY(program_id, primary_site_id),
  FOREIGN KEY(program_id) REFERENCES program(id),
  FOREIGN KEY(primary_site_id)  REFERENCES primary_site(id)
);

ALTER TABLE program ADD COLUMN date_updated TIMESTAMP NOT NULL;
ALTER TABLE program ADD COLUMN institutions VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE program ADD COLUMN countries VARCHAR(255) NOT NULL;
ALTER TABLE program ADD COLUMN regions VARCHAR(255) DEFAULT '';

ALTER TABLE program ALTER COLUMN submitted_donors SET DEFAULT 0;
ALTER TABLE program ALTER COLUMN genomic_donors SET DEFAULT 0;
ALTER TABLE program ALTER COLUMN commitment_donors SET DEFAULT 0;

ALTER TABLE program ALTER COLUMN membership_type TYPE membership USING membership_type :: membership;
ALTER TABLE program ALTER COLUMN membership_type SET NOT NULL;