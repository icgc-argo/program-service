package org.icgc.argo.program_service.repositories;

import java.util.Optional;
import java.util.UUID;
import org.icgc.argo.program_service.model.entity.CountryEntity;

public interface CountryRepository extends BaseRepository<CountryEntity, UUID> {

  Optional<CountryEntity> getCountryByName(String name);
}
