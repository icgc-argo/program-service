ALTER TABLE program_ego_group ADD COLUMN program_short_name VARCHAR(255);
UPDATE program_ego_group SET program_short_name=P.short_name
FROM program P, program_ego_group G
WHERE P.ID = G.program_id;

ALTER TABLE program_ego_group drop column program_id;
create UNIQUE index on program_ego_group (program_short_name, role);
