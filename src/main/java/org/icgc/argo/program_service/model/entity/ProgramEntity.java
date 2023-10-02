/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.model.entity;

import static org.icgc.argo.program_service.utils.CollectionUtils.mapToList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.PositiveOrZero;
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

  @Column(name = SqlFields.DATA_CENTER_ID)
  private UUID dataCenterId;

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

  @Column(name = SqlFields.IS_ACTIVE)
  private Boolean active;

  @Column(name = SqlFields.LEGACY_SHORTNAME)
  private String legacyShortName;

  @NotNull
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
      mappedBy = ProgramInstitution.Fields.program,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<ProgramInstitution> programInstitutions = new TreeSet<>();

  @NotNull
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
      mappedBy = ProgramCountry.Fields.program,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<ProgramCountry> programCountries = new TreeSet<>();

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
      mappedBy = ProgramCancer.Fields.program,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<@NotNull ProgramCancer> programCancers = new TreeSet<>();

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
      mappedBy = ProgramPrimarySite.Fields.program,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
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
}
