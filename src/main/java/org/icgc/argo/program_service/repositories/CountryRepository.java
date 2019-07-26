package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.CountryEntity;
import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends BaseRepository<CountryEntity, UUID>  {

  Optional<CountryEntity> getCountryByName(String name);

}
