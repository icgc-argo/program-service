-- CREATE TYPE join_program_invite_status AS ENUM ('PENDING', 'ACCEPTED', 'REVOKED');

CREATE TABLE join_program_invite
(
    id          UUID PRIMARY KEY,
    created_at  TIMESTAMP                  NOT NULL,
    accepted_at TIMESTAMP,
    expired_at  TIMESTAMP                  NOT NULL,
    program_id  UUID REFERENCES program    NOT NULL,
    user_email  TEXT                       NOT NULL,
    first_name  TEXT                       NOT NULL,
    last_name   TEXT                       NOT NULL,
    role        VARCHAR(32)                NOT NULL,
    email_sent  BOOLEAN                    NOT NULL,
    status      VARCHAR(32)                NOT NULL
);

CREATE TABLE program_ego_group
(
    id           UUID PRIMARY KEY,
    program_id   UUID REFERENCES program NOT NULL,
    role         VARCHAR(32)             NOT NULL,
    ego_group_id UUID                    NOT NULL
);
