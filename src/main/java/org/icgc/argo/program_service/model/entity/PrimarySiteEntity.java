package org.icgc.argo.program_service.model.entity;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;

@Entity
@Table(name = Tables.SITE)
@Builder
@Data
@EqualsAndHashCode
@ToString
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class PrimarySiteEntity implements NameableEntity<UUID> {

  @Id
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @Column(name = SqlFields.ID)
  @GenericGenerator(name = "site_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "site_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME)
  private String name;

  @Builder.Default
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(
          mappedBy = ProgramPrimarySite.Fields.site,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true)
  private Set<ProgramPrimarySite> programs = newHashSet();

}
