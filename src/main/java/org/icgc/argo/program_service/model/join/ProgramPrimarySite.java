package org.icgc.argo.program_service.model.join;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.Identifiable;
import org.icgc.argo.program_service.model.entity.PrimarySite;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import javax.persistence.*;

@Entity
@Data
@Table(name = Tables.PROGRAM_PRIMARY_SITE)
@Builder
@EqualsAndHashCode
@FieldNameConstants
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProgramPrimarySite implements Identifiable<ProgramPrimarySiteId> {

  @EmbeddedId
  private ProgramPrimarySiteId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramPrimarySiteId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private Program program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramPrimarySiteId.Fields.siteId)
  @JoinColumn(name = SqlFields.SITEID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private PrimarySite site;

}
