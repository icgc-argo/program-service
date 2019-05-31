package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;
import java.util.UUID;

public interface CancerRepository extends BaseRepository<CancerEntity, UUID> {

  Optional<CancerEntity> getCancerByName(String name);

}
