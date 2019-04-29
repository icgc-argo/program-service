package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface ProgramRepository extends JpaRepository<ProgramEntity, UUID> {


}
