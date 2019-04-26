package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgramEgoGroupRepository extends JpaRepository<ProgramEgoGroup, UUID> {
  Optional<ProgramEgoGroup> findByProgramAndRole(Program program, UserRole role);
}
