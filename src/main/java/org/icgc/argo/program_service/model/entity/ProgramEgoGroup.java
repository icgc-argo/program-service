package org.icgc.argo.program_service.model.entity;

import lombok.Getter;
import org.icgc.argo.program_service.UserRole;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.util.UUID;

@Entity
public class ProgramEgoGroup {

  @Id
  private UUID id;

  @ManyToOne
  @Getter
  private Program program;

  @Column(nullable = false, updatable = false)
  private UserRole role;

  @Column(nullable = false, updatable = false)
  private UUID egoGroupId;

  public ProgramEgoGroup(Program program, UserRole role, UUID egoGroupId) {
    this.program = program;
    this.role = role;
    this.egoGroupId = egoGroupId;
  }
}


