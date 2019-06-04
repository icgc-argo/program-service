package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CancerRepository extends JpaRepository<CancerEntity, UUID> {

  Optional<CancerEntity> getCancerByName(String name);

}
