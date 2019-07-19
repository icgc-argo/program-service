package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.InstitutionEntity;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends BaseRepository<InstitutionEntity, UUID> {

  Optional<InstitutionEntity> getInstitutionByName(String name);

}
