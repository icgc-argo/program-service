package org.icgc.argo.program_service.repositories;

import java.util.Optional;
import java.util.UUID;
import org.icgc.argo.program_service.model.entity.CancerEntity;

public interface CancerRepository extends BaseRepository<CancerEntity, UUID> {

  Optional<CancerEntity> getCancerByName(String name);
}
