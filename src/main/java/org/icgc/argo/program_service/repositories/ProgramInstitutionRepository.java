package org.icgc.argo.program_service.repositories;

import java.util.UUID;
import org.icgc.argo.program_service.model.join.ProgramInstitution;
import org.icgc.argo.program_service.model.join.ProgramInstitutionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProgramInstitutionRepository
    extends JpaRepository<ProgramInstitution, ProgramInstitutionId>,
        JpaSpecificationExecutor<ProgramInstitution> {

  void deleteAllByProgramId(UUID programId);

  void deleteByProgramIdAndInstitutionId(UUID programId, UUID institutionId);
}
