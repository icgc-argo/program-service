package org.icgc.argo.program_service.repositories;

import java.util.UUID;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramCancerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProgramCancerRepository
    extends JpaRepository<ProgramCancer, ProgramCancerId>, JpaSpecificationExecutor<ProgramCancer> {

  void deleteAllByProgramId(UUID programId);

  void deleteByProgramIdAndCancerId(UUID programId, UUID cancerId);
}
