package org.icgc.argo.program_service.model.entity;

import lombok.Getter;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.enums.Tables;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = Tables.PROGRAM_EGO_GROUP)
public class ProgramEgoGroupEntity {

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @Getter
  private ProgramEntity program;

  @Column(nullable = false, updatable = false)
  private UserRole role;

  @Column(nullable = false, updatable = false)
  private UUID egoGroupId;

  public ProgramEgoGroupEntity(ProgramEntity program, UserRole role, UUID egoGroupId) {
    this.program = program;
    this.role = role;
    this.egoGroupId = egoGroupId;
  }
}


