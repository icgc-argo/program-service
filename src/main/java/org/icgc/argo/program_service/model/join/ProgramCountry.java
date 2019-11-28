package org.icgc.argo.program_service.model.join;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Optional;
import javax.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.CountryEntity;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.jetbrains.annotations.NotNull;

@Entity
@Data
@Table(name = Tables.PROGRAM_COUNTRY)
@Builder
@EqualsAndHashCode
@ToString
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramCountry
    implements IdentifiableEntity<ProgramCountryId>, Comparable<ProgramCountry> {

  @EmbeddedId private ProgramCountryId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramCountryId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private ProgramEntity program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramCountryId.Fields.countryId)
  @JoinColumn(name = SqlFields.COUNTRYID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private CountryEntity country;

  public static Optional<ProgramCountry> createProgramCountry(
      @NonNull ProgramEntity p, @NonNull CountryEntity c) {
    if (c.getId() == null || isNullOrEmpty(c.getName())) {
      return Optional.empty();
    }
    val programCountry =
        ProgramCountry.builder()
            .id(ProgramCountryId.builder().programId(p.getId()).countryId(c.getId()).build())
            .program(p)
            .country(c)
            .build();
    return Optional.of(programCountry);
  }

  @Override
  public int compareTo(@NotNull ProgramCountry o) {
    return this.country.getName().compareTo(o.country.getName());
  }
}
