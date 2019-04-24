package org.icgc.argo.program_service.model.join;

import lombok.*;

import org.icgc.argo.program_service.model.entity.Cancer;
import org.icgc.argo.program_service.model.entity.Identifiable;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.model.enums.JavaFields;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;

import javax.persistence.*;

@Entity
@Data
@Table(name = Tables.PROGRAM_CANCER)
@Builder
@EqualsAndHashCode(of = "name")
@NoArgsConstructor
@AllArgsConstructor
public class ProgramCancer implements Identifiable<ProgramCancerId> {

  @EmbeddedId private ProgramCancerId id;

  @MapsId(value = JavaFields.PROGRAM_ID)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private Program program;

  @MapsId(value = JavaFields.CANCER_ID)
  @JoinColumn(name = SqlFields.CANCERID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private Cancer cancer;

}
