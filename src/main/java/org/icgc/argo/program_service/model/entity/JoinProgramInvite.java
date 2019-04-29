package org.icgc.argo.program_service.model.entity;

import lombok.Getter;
import lombok.Setter;
import org.icgc.argo.program_service.UserRole;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
public class JoinProgramInvite {

  public enum Status {PENDING, ACCEPTED, REVOKED};

  @Id
  private UUID id;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime expiredAt;

  private LocalDateTime acceptedAt;

  @ManyToOne
  private ProgramEntity program;

  @Column(nullable = false, updatable = false)
  @Getter private String userEmail;

  @Column(nullable = false)
  @Getter private String firstName;

  @Column(nullable = false)
  @Getter private String lastName;

  @Column(nullable = false)
  private UserRole role;

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

