package org.icgc.argo.program_service.repositories.query;

import lombok.NonNull;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.entity.RegionEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.join.ProgramRegion;
import org.springframework.data.jpa.domain.Specification;
import javax.persistence.criteria.Join;
import java.util.UUID;

public class RegionSpecification {

  public static Specification<RegionEntity> containsProgram(@NonNull UUID programId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<RegionEntity, ProgramRegion> prJoin = root.join("programRegions");
      Join<ProgramRegion, ProgramEntity> programJoin = prJoin.join("program");
      return builder.equal(programJoin.<Integer>get(SqlFields.ID), programId);
    };
  }

}
