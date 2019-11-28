package org.icgc.argo.program_service.repositories;

import java.util.Optional;
import java.util.UUID;
import org.icgc.argo.program_service.model.entity.RegionEntity;

public interface RegionRepository extends BaseRepository<RegionEntity, UUID> {

  Optional<RegionEntity> getRegionByName(String name);
}
