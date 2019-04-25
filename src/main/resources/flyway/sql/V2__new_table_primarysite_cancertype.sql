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
  site_id               UUID  NOT NULL,

  PRIMARY KEY(program_id, site_id),
  FOREIGN KEY(program_id) REFERENCES program(id),
  FOREIGN KEY(site_id)  REFERENCES primary_site(id)
);
