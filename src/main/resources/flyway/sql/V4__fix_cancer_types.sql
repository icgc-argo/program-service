-- CREATE TYPE join_program_invite_status AS ENUM ('PENDING', 'ACCEPTED', 'REVOKED');

DROP TABLE cancer CASCADE;
DROP TABLE primary_site CASCADE;
DROP TABLE program_cancer CASCADE;
DROP TABLE program_primary_site CASCADE;

create table program_cancer_type (
    program_id   UUID NOT NULL,
    cancer_type    VARCHAR(100) NOT NULL,
    PRIMARY KEY(program_id, cancer_type),
    FOREIGN KEY(program_id) REFERENCES program(id)
);

create table program_primary_site (
    program_id uuid NOT NULL,
    primary_site VARCHAR(100) NOT NULL,
    PRIMARY KEY(program_id, primary_site),
    FOREIGN KEY(program_id) REFERENCES program(id)
);