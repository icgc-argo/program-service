package org.icgc.argo.program_service.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.enums.Tables;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = Tables.PROGRAM_EGO_GROUP)
@FieldNameConstants
@NoArgsConstructor
public class ProgramEgoGroupEntity {

  @Id
  @Getter
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  @Getter
  private ProgramEntity program;

  @Column(nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  private UserRole role;

  @Column(nullable = false, updatable = false)
  @Getter
  private UUID egoGroupId;

  public ProgramEgoGroupEntity(ProgramEntity program, UserRole role, UUID egoGroupId) {
    this.program = program;
    this.role = role;
    this.egoGroupId = egoGroupId;
  }
}


