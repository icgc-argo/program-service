package org.icgc.argo.program_service.model.join;

import lombok.*;

import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.Cancer;
import org.icgc.argo.program_service.model.entity.Identifiable;
import org.icgc.argo.program_service.model.entity.Program;
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
public class ProgramCancer implements Identifiable<ProgramCancerId> {

  @EmbeddedId
  private ProgramCancerId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramCancerId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private Program program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramCancerId.Fields.cancerId)
  @JoinColumn(name = SqlFields.CANCERID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private Cancer cancer;

}
