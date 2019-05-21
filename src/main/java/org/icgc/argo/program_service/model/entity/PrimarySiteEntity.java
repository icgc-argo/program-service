package org.icgc.argo.program_service.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;

@Entity
@Table(name = Tables.SITE)
@Data
@Accessors(chain = true)
@FieldNameConstants
public class PrimarySiteEntity implements NameableEntity<UUID> {

  @Id
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Column(name = SqlFields.ID)
  @GenericGenerator(name = "site_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "site_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME)
  private String name;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
          mappedBy = ProgramPrimarySite.Fields.primarySite,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true)
  private Set<ProgramPrimarySite> programPrimarySites = newHashSet();

}
