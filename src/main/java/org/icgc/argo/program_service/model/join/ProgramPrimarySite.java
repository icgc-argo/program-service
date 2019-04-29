package org.icgc.argo.program_service.model.join;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
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
public class ProgramPrimarySite implements IdentifiableEntity<ProgramPrimarySiteId> {

  @EmbeddedId
  private ProgramPrimarySiteId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramPrimarySiteId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private ProgramEntity program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramPrimarySiteId.Fields.siteId)
  @JoinColumn(name = SqlFields.SITEID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private PrimarySiteEntity site;

}
