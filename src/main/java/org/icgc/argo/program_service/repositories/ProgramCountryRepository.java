package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.join.ProgramCountry;
import org.icgc.argo.program_service.model.join.ProgramCountryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ProgramCountryRepository extends JpaRepository<ProgramCountry, ProgramCountryId>,
        JpaSpecificationExecutor<ProgramCountry> {

  void deleteAllByProgramId(UUID programId);

}