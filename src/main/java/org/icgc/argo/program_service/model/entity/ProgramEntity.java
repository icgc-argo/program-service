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
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.URL;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.model.join.*;
import org.icgc.argo.program_service.proto.MembershipType;
import org.icgc.argo.program_service.validation.ProgramShortName;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import static org.icgc.argo.program_service.utils.CollectionUtils.mapToList;

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
  @ProgramShortName
  private String shortName;

  @NotNull
  @Column(name = SqlFields.NAME)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = SqlFields.MEMBERSHIPTYPE)
  private MembershipType membershipType;

  @NotNull
  @PositiveOrZero
  @Column(name = SqlFields.COMMITMENTDONORS)
  private Integer commitmentDonors;

  @NotNull
  @PositiveOrZero
  @Column(name = SqlFields.SUBMITTEDDONORS)
  private Integer submittedDonors;

  @NotNull
  @PositiveOrZero
  @Column(name = SqlFields.GENOMICDONORS)
  private Integer genomicDonors;

  @NotNull
  @URL
  @Column(name = SqlFields.WEBSITE)
  private String website;

  @NotNull
  @EqualsAndHashCode.Exclude
  @PastOrPresent
  @Column(name = SqlFields.CREATEDAT)
  private LocalDateTime createdAt;

  @NotNull
  @EqualsAndHashCode.Exclude
  @PastOrPresent
  @Column(name = SqlFields.UPDATEDAT)
  private LocalDateTime updatedAt;

  @Column(name = SqlFields.DESCRIPTION)
  private String description;

  @NotNull
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
    mappedBy = ProgramInstitution.Fields.program,
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true
  )
  private Set<ProgramInstitution> programInstitutions = new TreeSet<>();

  @NotNull
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
    mappedBy = ProgramCountry.Fields.program,
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true
  )
  private Set<ProgramCountry> programCountries = new TreeSet<>();

  @NotNull
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
    mappedBy = ProgramRegion.Fields.program,
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true
  )
  private Set<ProgramRegion> programRegions = new TreeSet<>();

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
    mappedBy = ProgramCancer.Fields.program,
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true
  )
  private Set<@NotNull ProgramCancer> programCancers = new TreeSet<>();

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
    mappedBy = ProgramPrimarySite.Fields.program,
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    orphanRemoval = true
  )

  private Set<@NotNull ProgramPrimarySite> programPrimarySites = new TreeSet<>();

  public List<String> listCancerTypes() {
    return mapToList(getProgramCancers(), c -> c.getCancer().getName());
  }

  public List<String> listPrimarySites() {
    return mapToList(getProgramPrimarySites(), p -> p.getPrimarySite().getName());
  }

  public List<String> listInstitutions() {
    return mapToList(getProgramInstitutions(), i -> i.getInstitution().getName());
  }

  public List<String> listCountries() {
    return mapToList(getProgramCountries(), c -> c.getCountry().getName());
  }

  public List<String> listRegions() {
    return mapToList(getProgramRegions(), r -> r.getRegion().getName());
  }

}
