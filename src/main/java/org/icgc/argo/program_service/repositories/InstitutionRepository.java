package org.icgc.argo.program_service.repositories;

import java.util.Optional;
import java.util.UUID;
import org.icgc.argo.program_service.model.entity.InstitutionEntity;

public interface InstitutionRepository extends BaseRepository<InstitutionEntity, UUID> {

  Optional<InstitutionEntity> getInstitutionByName(String name);
}
