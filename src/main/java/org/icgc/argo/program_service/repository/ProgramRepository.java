package org.icgc.argo.program_service.repository;

import org.icgc.argo.program_service.model.entity.ProgramDao;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface ProgramRepository extends CrudRepository<ProgramDao, UUID> {


}
