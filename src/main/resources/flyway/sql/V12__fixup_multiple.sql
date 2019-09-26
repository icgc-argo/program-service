CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DELETE FROM institution WHERE NAME='Multiple';

INSERT INTO primary_site (id, name)
VALUES (uuid_generate_v4(), 'Multiple')
ON CONFLICT DO NOTHING;

