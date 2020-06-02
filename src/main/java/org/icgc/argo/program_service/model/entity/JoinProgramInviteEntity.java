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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.PastOrPresent;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.enums.Tables;
import org.icgc.argo.program_service.proto.UserRole;

@Entity
@Data
@Table(name = Tables.JOIN_PROGRAM_INVITE)
@Accessors(chain = true)
@FieldNameConstants
@NoArgsConstructor
@Valid
public class JoinProgramInviteEntity {

  public enum Status {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED,
    INVALID
  }

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, updatable = false)
  @PastOrPresent
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @PastOrPresent private LocalDateTime acceptedAt;

  @ManyToOne @Getter private ProgramEntity program;

  @Column(nullable = false, updatable = false)
  @Email
  @Getter
  private String userEmail;

  @Column(nullable = false)
  @Getter
  private String firstName;

  @Column(nullable = false)
  @Getter
  private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Getter
  private UserRole role;

  @Column(nullable = false)
  @Getter
  @Setter
  private Boolean emailSent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  public Status getStatus() {
    if (status == Status.PENDING && isExpired()) {
      status = Status.EXPIRED;
    }
    return status;
  }

  public JoinProgramInviteEntity(
      ProgramEntity program, String userEmail, String firstName, String lastName, UserRole role) {
    this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    this.expiresAt = this.createdAt.plusDays(30);
    this.acceptedAt = null;

    this.program = program;
    this.userEmail = userEmail;
    this.firstName = firstName;
    this.lastName = lastName;
    this.role = role;
    this.emailSent = false;
    this.status = Status.PENDING;
  }

  public void revoke() {
    this.status = Status.REVOKED;
  }

  public void accept() {
    this.status = Status.ACCEPTED;
    this.acceptedAt = LocalDateTime.now(ZoneOffset.UTC);
  }

  public Boolean isExpired() {
    return LocalDateTime.now(ZoneOffset.UTC).isAfter(this.expiresAt);
  }
}
