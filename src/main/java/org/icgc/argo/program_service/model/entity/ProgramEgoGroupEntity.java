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

import java.util.UUID;
import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.validation.ProgramShortName;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@Table(name = Tables.PROGRAM_EGO_GROUP)
public class ProgramEgoGroupEntity implements Comparable<ProgramEgoGroupEntity> {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ProgramShortName
  @Column(nullable = false, unique = true, updatable = false)
  private String programShortName;

  @Column(nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  private UserRole role;

  @Column(nullable = false, updatable = false)
  private UUID egoGroupId;

  public int compareTo(ProgramEgoGroupEntity o) {
    return this.role.compareTo(o.role);
  }
}
