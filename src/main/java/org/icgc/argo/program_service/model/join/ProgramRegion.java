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

package org.icgc.argo.program_service.model.join;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Optional;
import javax.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.entity.RegionEntity;
import org.icgc.argo.program_service.model.enums.SqlFields;
import org.icgc.argo.program_service.model.enums.Tables;
import org.jetbrains.annotations.NotNull;

@Entity
@Data
@Table(name = Tables.PROGRAM_REGION)
@Builder
@EqualsAndHashCode
@ToString
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramRegion
    implements IdentifiableEntity<ProgramRegionId>, Comparable<ProgramRegion> {

  @EmbeddedId private ProgramRegionId id;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramRegionId.Fields.programId)
  @JoinColumn(name = SqlFields.PROGRAMID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private ProgramEntity program;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @MapsId(value = ProgramRegionId.Fields.regionId)
  @JoinColumn(name = SqlFields.REGIONID_JOIN)
  @ManyToOne(fetch = FetchType.LAZY)
  private RegionEntity region;

  public static Optional<ProgramRegion> createProgramRegion(
      @NonNull ProgramEntity p, @NonNull RegionEntity c) {
    if (c.getId() == null || isNullOrEmpty(c.getName())) {
      return Optional.empty();
    }

    val programRegion =
        ProgramRegion.builder()
            .id(ProgramRegionId.builder().programId(p.getId()).regionId(c.getId()).build())
            .program(p)
            .region(c)
            .build();
    return Optional.of(programRegion);
  }

  @Override
  public int compareTo(@NotNull ProgramRegion o) {
    return this.region.getName().compareTo(o.region.getName());
  }
}
