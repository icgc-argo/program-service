package org.icgc.argo.program_service.repositories;

import java.util.Optional;
import java.util.UUID;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;

public interface PrimarySiteRepository extends BaseRepository<PrimarySiteEntity, UUID> {

  Optional<PrimarySiteEntity> getPrimarySiteByName(String name);
}
