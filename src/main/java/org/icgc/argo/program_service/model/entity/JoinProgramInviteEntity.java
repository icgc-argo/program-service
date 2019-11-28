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
