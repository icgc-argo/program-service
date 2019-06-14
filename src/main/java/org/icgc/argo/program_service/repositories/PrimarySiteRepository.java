package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PrimarySiteRepository extends JpaRepository<PrimarySiteEntity, UUID>,
  JpaSpecificationExecutor<PrimarySiteEntity> {
  Optional<PrimarySiteEntity> getPrimarySiteByName(String name);
}