CREATE TYPE join_program_invitation_status AS ENUM ('pending', 'accepted');

CREATE TABLE join_program_invitation
(
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP               NOT NULL,
    accepted_at TIMESTAMP,
    program_id  UUID REFERENCES program NOT NULL,
    user_email  TEXT                    NOT NULL,
    emailSent   BOOLEAN                 NOT NULL,
    status      join_program_invitation_status
);
