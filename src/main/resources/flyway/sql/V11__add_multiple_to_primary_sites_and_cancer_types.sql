CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO institution (id, name)
VALUES (uuid_generate_v4(), 'Multiple')
ON CONFLICT DO NOTHING;

INSERT INTO cancer (id, name)
VALUES (uuid_generate_v4(), 'Multiple')
ON CONFLICT DO NOTHING;
