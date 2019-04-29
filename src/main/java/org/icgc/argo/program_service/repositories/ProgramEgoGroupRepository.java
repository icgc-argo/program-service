package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgramEgoGroupRepository extends JpaRepository<ProgramEgoGroupEntity, UUID> {
  Optional<ProgramEgoGroupEntity> findByProgramAndRole(ProgramEntity program, UserRole role);
}
