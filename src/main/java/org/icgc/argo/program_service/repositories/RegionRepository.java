package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.RegionEntity;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends BaseRepository<RegionEntity, UUID> {

  Optional<RegionEntity> getRegionByName(String name);

}
