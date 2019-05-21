package org.icgc.argo.program_service.model.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.enums.Tables;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@FieldNameConstants
@Accessors(chain = true)
@Table(name = Tables.PROGRAM_EGO_GROUP)
public class ProgramEgoGroupEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @ManyToOne
  private ProgramEntity program;

  @Column(nullable = false, updatable = false)
  @Enumerated(EnumType.STRING)
  @Getter
  private UserRole role;

  @Column(nullable = false, updatable = false)
  private UUID egoGroupId;

}


