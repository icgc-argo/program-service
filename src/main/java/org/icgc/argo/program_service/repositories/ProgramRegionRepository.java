package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.join.ProgramRegion;
import org.icgc.argo.program_service.model.join.ProgramRegionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ProgramRegionRepository extends JpaRepository<ProgramRegion, ProgramRegionId>,
        JpaSpecificationExecutor<ProgramRegion> {

  void deleteAllByProgramId(UUID programId);
}
