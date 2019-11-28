package org.icgc.argo.program_service.repositories;

import java.util.UUID;
import org.icgc.argo.program_service.model.join.ProgramRegion;
import org.icgc.argo.program_service.model.join.ProgramRegionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProgramRegionRepository
    extends JpaRepository<ProgramRegion, ProgramRegionId>, JpaSpecificationExecutor<ProgramRegion> {

  void deleteAllByProgramId(UUID programId);

  void deleteByProgramIdAndRegionId(UUID programId, UUID regionId);
}
