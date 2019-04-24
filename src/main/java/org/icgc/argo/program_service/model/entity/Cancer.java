package org.icgc.argo.program_service.model.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.icgc.argo.program_service.model.enums.JavaFields;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.ProgramCancer;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;


@Entity
@Table(name = Tables.CANCER)
@Builder
@Data
@EqualsAndHashCode(of = "name")
@AllArgsConstructor
@NoArgsConstructor
public class Cancer implements NameableEntity<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "cancer_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "cancer_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME, nullable = false, unique = true)
  private String name;

  @Builder.Default
  @OneToMany(
          mappedBy = JavaFields.PROGRAM,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true)
  private Set<ProgramCancer> programs = newHashSet();

}
