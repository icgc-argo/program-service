CREATE TYPE join_program_invitation_status AS ENUM ('PENDING', 'ACCEPTED', 'REVOKED');
CREATE TYPE user_role AS ENUM ('ADMIN', 'MEMBER', 'COLLABORATOR');

CREATE TABLE join_program_invitation
(
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP               NOT NULL,
    accepted_at TIMESTAMP,
    expired_at  TIMESTAMP               NOT NULL,
    program_id  UUID REFERENCES program NOT NULL,
    user_email  TEXT                    NOT NULL,
    first_name  TEXT                    NOT NULL,
    last_name   TEXT                    NOT NULL,
    role        user_role               NOT NULL,
    emailSent   BOOLEAN                 NOT NULL,
    status      join_program_invitation_status NOT NULL ,
    UNIQUE (program_id, user_email)
);
