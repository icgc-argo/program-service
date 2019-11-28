package org.icgc.argo.program_service.repositories.query;

import java.util.UUID;
import javax.persistence.criteria.Join;
import lombok.NonNull;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.springframework.data.jpa.domain.Specification;

public class CancerSpecification {

  public static Specification<CancerEntity> containsProgram(@NonNull UUID programId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<CancerEntity, ProgramCancer> psJoin = root.join("programCancers");
      Join<ProgramCancer, ProgramEntity> programJoin = psJoin.join("program");
      return builder.equal(programJoin.<Integer>get(SqlFields.ID), programId);
    };
  }
}
