CREATE TABLE country (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE institution  (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE region  (
    id                  UUID PRIMARY KEY,
    name                VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE program_country (
  program_id           UUID NOT NULL,
  country_id           UUID NOT NULL,

  PRIMARY KEY(program_id,  country_id),
  FOREIGN KEY(program_id)  REFERENCES program(id),
  FOREIGN KEY(country_id)  REFERENCES country(id)
);

CREATE TABLE program_institution (
  program_id                UUID NOT NULL,
  institution_id            UUID NOT NULL,

  PRIMARY KEY(program_id, institution_id),
  FOREIGN KEY(program_id) REFERENCES program(id),
  FOREIGN KEY(institution_id)  REFERENCES institution(id)
);

CREATE TABLE program_region (
  program_id          UUID NOT NULL,
  region_id           UUID NOT NULL,

  PRIMARY KEY(program_id, region_id),
  FOREIGN KEY(program_id) REFERENCES program(id),
  FOREIGN KEY(region_id)  REFERENCES region(id)
);

ALTER TABLE PROGRAM
DROP COLUMN institutions,
DROP COLUMN countries,
DROP COLUMN regions  ;