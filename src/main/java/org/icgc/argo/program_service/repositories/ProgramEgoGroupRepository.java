package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgramEgoGroupRepository extends JpaRepository<ProgramEgoGroupEntity, UUID> {
  Optional<ProgramEgoGroupEntity> findByProgramIdAndRole(UUID programId, UserRole role);

  List<ProgramEgoGroupEntity> findAllByProgramId(UUID programId);
}
