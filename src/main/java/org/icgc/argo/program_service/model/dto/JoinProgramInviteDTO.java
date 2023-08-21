package org.icgc.argo.program_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.icgc.argo.program_service.model.enums.InviteStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinProgramInviteDTO {

  private String id;
  private String createdAt;
  private String expiresAt;
  private String acceptedAt;
  private ProgramDTO program;
  private UserDTO user;
  private boolean emailSent;
  private InviteStatus status;
}
