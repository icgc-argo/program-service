package org.icgc.argo.program_service.model.join;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;

import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

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
  @MapsId(value = ProgramPrimarySiteId.Fields.primarySiteId)
  @JoinColumn(name = SqlFields.SITEID_JOIN)
  @ManyToOne(
          cascade = {CascadeType.PERSIST, CascadeType.MERGE},
          fetch = FetchType.LAZY)
  private PrimarySiteEntity primarySite;

  public static ProgramPrimarySite createProgramPrimarySite(@NonNull ProgramEntity p, @NonNull PrimarySiteEntity ps){
    return ProgramPrimarySite.builder()
        .id(ProgramPrimarySiteId.builder()
            .primarySiteId(ps.getId())
            .programId(p.getId())
            .build())
        .primarySite(ps)
        .program(p)
        .build();
  }
}
