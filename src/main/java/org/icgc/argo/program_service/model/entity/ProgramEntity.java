/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.icgc.argo.program_service.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import lombok.val;
import org.hibernate.annotations.GenericGenerator;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static org.icgc.argo.program_service.model.join.ProgramCancer.createProgramCancer;
import static org.icgc.argo.program_service.model.join.ProgramPrimarySite.createProgramPrimarySite;

@Entity
@Table(name = Tables.PROGRAM)
@Data
@Accessors(chain = true)
@FieldNameConstants
public class ProgramEntity implements NameableEntity<UUID> {

  @Id
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Column(name = SqlFields.ID)
  @GenericGenerator(name = "program_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "program_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.SHORTNAME)
  private String shortName;

  @NotNull
  @Column(name = SqlFields.NAME)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = SqlFields.MEMBERSHIPTYPE)
  private org.icgc.argo.program_service.MembershipType membershipType;

  @NotNull
  @Column(name = SqlFields.COMMITMENTDONORS)
  private Integer commitmentDonors;

  @NotNull
  @Column(name = SqlFields.SUBMITTEDDONORS)
  private Integer submittedDonors;

  @NotNull
  @Column(name = SqlFields.GENOMICDONORS)
  private Integer genomicDonors;

  @NotNull
  @Column(name = SqlFields.WEBSITE)
  private String website;

  @NotNull
  @EqualsAndHashCode.Exclude
  @Column(name = SqlFields.CREATEDAT)
  private LocalDateTime createdAt;

  @NotNull
  @EqualsAndHashCode.Exclude
  @Column(name = SqlFields.UPDATEDAT)
  private LocalDateTime updatedAt;

  @NotNull
  @Column(name = SqlFields.INSTITUTIONS)
  private String institutions;

  @NotNull
  @Column(name = SqlFields.COUNTRIES)
  private String countries;

  @Column(name = SqlFields.REGIONS)
  private String regions;

  @Column(name = SqlFields.DESCRIPTION)
  private String description;


  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
          mappedBy = ProgramCancer.Fields.program,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true
  )
  private Set<ProgramCancer> programCancers = newHashSet();

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
          mappedBy = ProgramPrimarySite.Fields.program,
          cascade = CascadeType.ALL,
          fetch = FetchType.LAZY,
          orphanRemoval = true
  )
  private Set<ProgramPrimarySite> programPrimarySites = newHashSet();

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(
          mappedBy = ProgramEgoGroupEntity.Fields.program,
          cascade = CascadeType.ALL,
          fetch = FetchType.EAGER,
          orphanRemoval = true
  )
  private Set<ProgramEgoGroupEntity> egoGroups = newHashSet();

  public void associateCancer(@NonNull CancerEntity c){
    val pc = createProgramCancer(this, c);
    this.getProgramCancers().add(pc);
    c.getProgramCancers().add(pc);
  }

  public void associatePrimarySite(@NonNull PrimarySiteEntity ps){
    val pps = createProgramPrimarySite(this, ps);
    this.getProgramPrimarySites().add(pps);
    ps.getProgramPrimarySites().add(pps);
  }

  public void associateEgoGroup(@NonNull ProgramEgoGroupEntity e){
    this.getEgoGroups().add(e);
    e.setProgram(this);
  }

}
