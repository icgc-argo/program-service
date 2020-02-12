ALTER TABLE program
    DROP COLUMN active,
    DROP COLUMN legacy_short_name;

DELETE FROM flyway_schema_history WHERE version=14;