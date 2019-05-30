package org.icgc.argo.program_service.model.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.enums.Tables;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Data
@Table(name = Tables.JOIN_PROGRAM_INVITE)
@Accessors(chain = true)
@FieldNameConstants
@NoArgsConstructor
public class JoinProgramInvite {

  public enum Status {PENDING, ACCEPTED, REVOKED};

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime expiredAt;

  private LocalDateTime acceptedAt;

  @ManyToOne
  @Getter
  private ProgramEntity program;

  @Column(nullable = false, updatable = false)
  @Getter private String userEmail;

  @Column(nullable = false)
  @Getter private String firstName;

  @Column(nullable = false)
  @Getter private String lastName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Getter private UserRole role;

  @Column(nullable = false)
  @Getter @Setter
  private Boolean emailSent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status;

  public JoinProgramInvite(ProgramEntity program, String userEmail, String firstName, String lastName, UserRole role) {
    this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    this.expiredAt = this.createdAt.plusHours(48);
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
    return LocalDateTime.now(ZoneOffset.UTC).isAfter(this.expiredAt);
  }
}

