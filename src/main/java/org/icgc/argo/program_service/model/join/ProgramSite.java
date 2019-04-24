package org.icgc.argo.program_service.model.join;

import lombok.*;
import org.icgc.argo.program_service.model.entity.Identifiable;
import org.icgc.argo.program_service.model.entity.PrimarySite;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.model.enums.JavaFields;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;

import javax.persistence.*;

@Entity
@Data
@Table(name = Tables.PROGRAM_SITE)
@Builder
@EqualsAndHashCode(of = "name")
@NoArgsConstructor
@AllArgsConstructor
public class ProgramSite implements Identifiable<ProgramSiteId> {

  @EmbeddedId
  private ProgramSiteId id;

  @MapsId(value = JavaFields.PROGRAM_ID)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private Program program;

  @MapsId(value = JavaFields.SITE_ID)
  @JoinColumn(name = SqlFields.SITEID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private PrimarySite site;

}
