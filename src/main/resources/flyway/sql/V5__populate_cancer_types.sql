ALTER TABLE program_ego_group add column program_short_name VARCHAR(255);
UPDATE program_ego_group SET program_short_name=P.short_name
from program P, program_ego_group G
where P.ID = G.program_id;
alter table program_ego_group drop column program_id;
