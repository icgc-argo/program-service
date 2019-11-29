package org.icgc.argo.program_service.model.entity;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.ProgramRegion;

@Entity
@Table(name = Tables.REGION)
@Data
@Accessors(chain = true)
@FieldNameConstants
public class RegionEntity implements NameableEntity<UUID> {

  @Id
  @Column(name = SqlFields.ID)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @GenericGenerator(name = "region_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "region_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME)
  private String name;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(
      mappedBy = ProgramRegion.Fields.region,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<ProgramRegion> programRegions = newHashSet();
}
