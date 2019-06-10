/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.icgc.argo.program_service.model.join;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import javax.persistence.*;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

@Entity
@Data
@Table(name = Tables.PROGRAM_PRIMARY_SITE)
@Builder
@EqualsAndHashCode
@FieldNameConstants
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProgramPrimarySite implements IdentifiableEntity<ProgramPrimarySiteId> {

  @EmbeddedId
  private ProgramPrimarySiteId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramPrimarySiteId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(fetch = FetchType.EAGER)
  private ProgramEntity program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramPrimarySiteId.Fields.primarySiteId)
  @JoinColumn(name = SqlFields.SITEID_JOIN)
  @ManyToOne(fetch = FetchType.EAGER)
  private PrimarySiteEntity primarySite;

  public static Optional<ProgramPrimarySite> createProgramPrimarySite(@NonNull ProgramEntity p, @NonNull PrimarySiteEntity ps){
    if(ps.getId() == null || isNullOrEmpty(ps.getName())){
      return Optional.empty();
    }
    val programPrimarySite = ProgramPrimarySite.builder()
        .id(ProgramPrimarySiteId.builder()
            .primarySiteId(ps.getId())
            .programId(p.getId())
            .build())
        .primarySite(ps)
        .program(p)
        .build();
    return Optional.of(programPrimarySite);
  }

}
