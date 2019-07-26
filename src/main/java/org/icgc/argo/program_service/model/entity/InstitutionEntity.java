package org.icgc.argo.program_service.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.ProgramInstitution;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;

@Entity
@Table(name = Tables.INSTITUTION)
@Data
@Accessors(chain = true)
@FieldNameConstants
public class InstitutionEntity implements NameableEntity<UUID> {

  @Id
  @Column(name = SqlFields.ID)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @GenericGenerator(name = "institution_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "institution_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME)
  private String name;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(
          mappedBy = ProgramInstitution.Fields.institution,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true)
  private Set<ProgramInstitution> programInstitutions = newHashSet();

}
