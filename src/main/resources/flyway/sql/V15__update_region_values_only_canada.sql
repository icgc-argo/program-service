DELETE FROM program_region;
DELETE FROM region;

INSERT INTO region (id, name)
VALUES
    (uuid_generate_v4(), 'Canada');

INSERT INTO program_region (program_id, region_id)
WITH
    prog AS (
        SELECT id FROM program
    ),
    reg AS (
        SELECT id
        FROM region
        WHERE name = 'Canada'
    )
SELECT prog.id, reg.id
FROM prog, reg;

