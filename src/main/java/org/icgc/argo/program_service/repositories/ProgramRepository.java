package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;



public interface ProgramRepository extends JpaRepository<ProgramEntity, UUID>, JpaSpecificationExecutor<ProgramEntity> {
  Optional<ProgramEntity> findByName(String name);
}
