package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroup;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProgramEgoGroupRepository extends CrudRepository<ProgramEgoGroup, UUID> {
  Optional<ProgramEgoGroup> findbyProgramAndRole(Program program, UserRole role);
}
