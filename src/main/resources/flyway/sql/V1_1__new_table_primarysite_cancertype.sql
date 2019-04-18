CREATE TYPE membershiptype AS ENUM ('FULL', 'ASSOCIATE');

CREATE TABLE CANCER (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE SITE  (
    id                UUID PRIMARY KEY,
    name              VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE PROGRAMCANCER (
  program_id          UUID NOT NULL,
  cancer_id           UUID NOT NULL,
  PRIMARY KEY(program_id, cancer_id),
  FOREIGN KEY(program_id) REFERENCES PROGRAM(id),
  FOREIGN KEY(cancer_id)  REFERENCES CANCER(id)
);

CREATE TABLE PROGRAMSITE (
  program_id            UUID  NOT NULL,
  site_id               UUID  NOT NULL,
  PRIMARY KEY(program_id, site_id),
  FOREIGN KEY(program_id) REFERENCES PROGRAM(id),
  FOREIGN KEY(site_id)  REFERENCES SITE(id)
);

ALTER TABLE PROGRAM ADD COLUMN date_updated TIMESTAMP NOT NULL;
ALTER TABLE PROGRAM ADD COLUMN institutions VARCHAR(255) DEFAULT '';
ALTER TABLE PROGRAM ADD COLUMN countries VARCHAR(255) NOT NULL;
ALTER TABLE PROGRAM ADD COLUMN regions VARCHAR(255) DEFAULT '';
ALTER TABLE PROGRAM ALTER COLUMN membership_type TYPE membershiptype USING membership_type :: membershiptype;
ALTER TABLE PROGRAM ALTER COLUMN membership_type SET NOT NULL;