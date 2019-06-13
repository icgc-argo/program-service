package org.icgc.argo.program_service.repositories.query;

import lombok.NonNull;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Join;
import java.util.UUID;

public class PrimarySiteSpecification {

  public static Specification<PrimarySiteEntity> containsProgram(@NonNull UUID programId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<PrimarySiteEntity, ProgramPrimarySite> psJoin = root.join("programPrimarySites");
      Join<ProgramPrimarySite, ProgramEntity> programJoin = psJoin.join("program");
      return builder.equal(programJoin.<Integer>get(SqlFields.ID), programId);
    };
  }

}
