package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CancerRepository extends JpaRepository<CancerEntity, UUID>,
  JpaSpecificationExecutor<CancerEntity>
{
  Optional<CancerEntity> getCancerByName(String name);
  List<CancerEntity> findAllByNameIn(List<String> names);
}
