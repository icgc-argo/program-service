package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import java.util.Optional;
import java.util.UUID;

public interface PrimarySiteRepository extends BaseRepository<PrimarySiteEntity, UUID> {

  Optional<PrimarySiteEntity> getPrimarySiteByName(String name);

}