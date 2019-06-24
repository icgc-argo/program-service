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
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.icgc.argo.program_service.proto.MembershipType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Pattern;
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
  // Only capitals, dash permissible, lst two letters should be country code
  @Pattern(regexp = "^[-A-Z]+?(AD|AE|AF|AG|AI|AL|AM|AO|AQ|AR|AS|AT|AU|AW|AX|AZ|BA|BB|BD|BE|BF|BG|BH|BI|BJ|BL|BM|BN|BO|"
    + "BQ|BQ|BR|BS|BT|BV|BW|BY|BZ|CA|CC|CD|CF|CG|CH|CI|CK|CL|CM|CN|CO|CR|CU|CV|CW|CX|CY|CZ|DE|DJ|DK|DM|DO|DZ|EC|EE|EG|"
    + "EH|ER|ES|ET|FI|FJ|FK|FM|FO|FR|GA|GB|GD|GE|GF|GG|GH|GI|GL|GM|GN|GP|GQ|GR|GS|GT|GU|GW|GY|HK|HM|HN|HR|HT|HU|ID|IE|"
    + "IL|IM|IN|IO|IQ|IR|IS|IT|JE|JM|JO|JP|KE|KG|KH|KI|KM|KN|KP|KR|KW|KY|KZ|LA|LB|LC|LI|LK|LR|LS|LT|LU|LV|LY|MA|MC|MD|"
    + "ME|MF|MG|MH|MK|ML|MM|MN|MO|MP|MQ|MR|MS|MT|MU|MV|MW|MX|MY|MZ|NA|NC|NE|NF|NG|NI|NL|NO|NP|NR|NU|NZ|OM|PA|PE|PF|PG|"
    + "PH|PK|PL|PM|PN|PR|PS|PT|PW|PY|QA|RE|RO|RS|RU|RW|SA|SB|SC|SD|SE|SG|SH|SI|SJ|SK|SL|SM|SN|SO|SR|SS|ST|SV|SX|SY|SZ|"
    + "TC|TD|TF|TG|TH|TJ|TK|TL|TM|TN|TO|TR|TT|TV|TW|TZ|UA|UG|UM|US|UY|UZ|VA|VC|VE|VG|VI|VN|VU|WF|WS|YE|YT|ZA|ZM|ZW)$")
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
}
