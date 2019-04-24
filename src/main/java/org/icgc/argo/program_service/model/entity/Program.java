package org.icgc.argo.program_service.model.entity;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.icgc.argo.program_service.model.enums.JavaFields;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramSite;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;

@Entity
@Table(name = Tables.PROGRAM)
@Builder
@Data
@EqualsAndHashCode(of = "name")
@AllArgsConstructor
@NoArgsConstructor
public class Program implements NameableEntity<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "program_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "program_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.SHORTNAME, unique = true, nullable = false)
  private String shortName;

  @NotNull
  @Column(name = SqlFields.NAME, unique = true, nullable = false)
  private String name;

  @NotNull
  @Column(name = SqlFields.DESCRIPTION, nullable = false)
  private String description;

  @NotNull
  @Column(name = SqlFields.MEMBERSHIPTYPE, nullable = false)
  private org.icgc.argo.program_service.MembershipType membershipType;

  @NotNull
  @Column(name = SqlFields.COMMITMENTDONORS, nullable = false)
  private int commitmentDonors;

  @NotNull
  @Column(name = SqlFields.SUBMITTEDDONORS, nullable = false)
  private int submittedDonors;

  @NotNull
  @Column(name = SqlFields.GENOMICDONORS, nullable = false)
  private int genomicDonors;

  @NotNull
  @Column(name = SqlFields.WEBSITE, nullable = false)
  private String website;

  @NotNull
  @Column(name = SqlFields.DATECREATED, nullable = false)
  private Date dateCreated;

  @NotNull
  @Column(name = SqlFields.DATEUPDATED, nullable = false)
  private Date dateUpdated;

  @NotNull
  @Column(name = SqlFields.INSTITUTIONS, nullable = false)
  private String institutions;

  @Column(name = SqlFields.REGIONS)
  private String regions;

  @NotNull
  @Column(name = SqlFields.COUNTRIES)
  private String countries;

  @Builder.Default
  @OneToMany(
          mappedBy = JavaFields.PROGRAM,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true
  )
  private Set<ProgramCancer> cancers = newHashSet();

  @Builder.Default
  @OneToMany(
          mappedBy = JavaFields.PROGRAM,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true
  )
  private Set<ProgramSite> sites = newHashSet();

}
