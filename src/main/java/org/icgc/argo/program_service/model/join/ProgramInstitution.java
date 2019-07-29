package org.icgc.argo.program_service.model.join;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.InstitutionEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@Entity
@Data
@Table(name = Tables.PROGRAM_INSTITUTION)
@Builder
@EqualsAndHashCode
@ToString
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramInstitution implements IdentifiableEntity<ProgramInstitutionId>, Comparable<ProgramInstitution> {

  @EmbeddedId
  private ProgramInstitutionId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramInstitutionId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private ProgramEntity program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramInstitutionId.Fields.institutionId)
  @JoinColumn(name = SqlFields.INSTITUTIONID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private InstitutionEntity institution;

  public static Optional<ProgramInstitution> createProgramInstitution(@NonNull ProgramEntity p, @NonNull InstitutionEntity c) {
    if (c.getId() == null || isNullOrEmpty(c.getName())) {
      return Optional.empty();
    }

    val programInstitution = ProgramInstitution.builder()
            .id(ProgramInstitutionId.builder()
                    .programId(p.getId())
                    .institutionId(c.getId())
                    .build())
            .program(p)
            .institution(c)
            .build();
    return Optional.of(programInstitution);
  }

  @Override public int compareTo(@NotNull ProgramInstitution o) {
    return this.institution.getName().compareTo(o.institution.getName());
  }
}
