package org.icgc.argo.program_service.repositories.query;

import java.util.UUID;
import javax.persistence.criteria.Join;
import lombok.NonNull;
import org.icgc.argo.program_service.model.entity.InstitutionEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.join.ProgramInstitution;
import org.springframework.data.jpa.domain.Specification;

public class InstitutionSpecification {

  public static Specification<InstitutionEntity> containsProgram(@NonNull UUID programId) {
    return (root, query, builder) -> {
      query.distinct(true);
      Join<InstitutionEntity, ProgramInstitution> piJoin = root.join("programInstitutions");
      Join<ProgramInstitution, ProgramEntity> programJoin = piJoin.join("program");
      return builder.equal(programJoin.<Integer>get(SqlFields.ID), programId);
    };
  }
}
