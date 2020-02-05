INSERT INTO region (id, name)
VALUES
    (uuid_generate_v4(), 'Canada');

UPDATE program_region SET region_id = (SELECT id FROM region WHERE name = 'Canada');

DELETE FROM region WHERE NOT name = 'Canada';