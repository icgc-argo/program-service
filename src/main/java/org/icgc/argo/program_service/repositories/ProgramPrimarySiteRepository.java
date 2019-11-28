package org.icgc.argo.program_service.repositories;

import java.util.List;
import java.util.UUID;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.icgc.argo.program_service.model.join.ProgramPrimarySiteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProgramPrimarySiteRepository
    extends JpaRepository<ProgramPrimarySite, ProgramPrimarySiteId>,
        JpaSpecificationExecutor<ProgramPrimarySite> {

  void deleteAllByProgramId(UUID programId);

  List<ProgramPrimarySite> deleteByProgramIdAndPrimarySiteId(UUID programId, UUID primarySiteId);
}
