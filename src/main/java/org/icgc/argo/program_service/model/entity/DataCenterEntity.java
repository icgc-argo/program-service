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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.validator.constraints.URL;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;

@Entity
@Table(name = Tables.DATA_CENTER)
@Data
@Accessors(chain = true)
@FieldNameConstants
public class DataCenterEntity implements NameableEntity<UUID> {

  @Id
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Column(name = SqlFields.ID)
  @GenericGenerator(name = "datacenter_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "datacenter_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.SHORTNAME)
  private String shortName;

  @NotNull
  @Column(name = SqlFields.NAME)
  private String name;

  @NotNull
  @Column(name = SqlFields.ORGANIZATION)
  private String organization;

  @NotNull
  @Email
  @Column(name = SqlFields.EMAIL)
  private String email;

  @NotNull
  @URL
  @Column(name = SqlFields.UI_URL)
  private String uiUrl;

  @NotNull
  @URL
  @Column(name = SqlFields.GATEWAY_URL)
  private String gatewayUrl;

  @NotNull
  @Column(name = SqlFields.ANALYSIS_SONG_CODE)
  private String analysisSongCode;

  @NotNull
  @URL
  @Column(name = SqlFields.ANALYSIS_SONG_URL)
  private String analysisSongUrl;

  @NotNull
  @URL
  @Column(name = SqlFields.ANALYSIS_SCORE_URL)
  private String analysisScoreUrl;

  @NotNull
  @Column(name = SqlFields.SUBMISSION_SONG_CODE)
  private String submissionSongCode;

  @NotNull
  @URL
  @Column(name = SqlFields.SUBMISSION_SONG_URL)
  private String submissionSongUrl;

  @NotNull
  @URL
  @Column(name = SqlFields.SUBMISSION_SCORE_URL)
  private String submissionScoreUrl;

  @NotNull
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(
      mappedBy = ProgramEntity.Fields.dataCenterId,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<ProgramEntity> programEntites = new TreeSet<>();

  public List<String> listPrograms() {
    return mapToList(getProgramEntites(), i -> i.getName());
  }
}
