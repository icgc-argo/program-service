package org.icgc.argo.program_service.repositories.query;

import lombok.NonNull;
import org.icgc.argo.program_service.model.entity.CountryEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.join.ProgramCountry;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import java.util.UUID;

public class CountrySpecification {

  public static Specification<CountryEntity> containsProgram(@NonNull UUID programId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<CountryEntity, ProgramCountry> pcJoin = root.join("programCountries");
      Join<ProgramCountry, ProgramEntity> programJoin = pcJoin.join("program");
      return builder.equal(programJoin.<Integer>get(SqlFields.ID), programId);
    };
  }

}
