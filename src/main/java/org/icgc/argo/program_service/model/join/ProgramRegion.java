package org.icgc.argo.program_service.model.join;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.entity.RegionEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.jetbrains.annotations.NotNull;
import javax.persistence.*;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@Entity
@Data
@Table(name = Tables.PROGRAM_REGION)
@Builder
@EqualsAndHashCode
@ToString
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramRegion implements IdentifiableEntity<ProgramRegionId>, Comparable<ProgramRegion> {

  @EmbeddedId
  private ProgramRegionId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramRegionId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private ProgramEntity program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramRegionId.Fields.regionId)
  @JoinColumn(name = SqlFields.REGIONID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private RegionEntity region;

  public static Optional<ProgramRegion> createProgramRegion(@NonNull ProgramEntity p, @NonNull RegionEntity c) {
    if (c.getId() == null || isNullOrEmpty(c.getName())) {
      return Optional.empty();
    }

    val programRegion = ProgramRegion.builder()
            .id(ProgramRegionId.builder()
                    .programId(p.getId())
                    .regionId(c.getId())
                    .build())
            .program(p)
            .region(c)
            .build();
    return Optional.of(programRegion);
  }

  @Override public int compareTo(@NotNull ProgramRegion o) {
    return this.region.getName().compareTo(o.region.getName());
  }

}
