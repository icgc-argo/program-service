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
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import javax.persistence.*;

@Entity
@Data
@Table(name = Tables.PROGRAM_CANCER)
@Builder
@EqualsAndHashCode
@ToString
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramCancer implements IdentifiableEntity<ProgramCancerId> {

  @EmbeddedId
  private ProgramCancerId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramCancerId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private ProgramEntity program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramCancerId.Fields.cancerId)
  @JoinColumn(name = SqlFields.CANCERID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private CancerEntity cancer;

  public static ProgramCancer createProgramCancer(@NonNull ProgramEntity p, @NonNull CancerEntity c){
    return ProgramCancer.builder()
        .id(ProgramCancerId.builder()
            .programId(p.getId())
            .cancerId(c.getId())
            .build())
        .program(p)
        .cancer(c)
        .build();
  }
}
