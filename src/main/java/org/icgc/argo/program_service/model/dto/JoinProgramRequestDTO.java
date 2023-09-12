package org.icgc.argo.program_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JoinProgramRequestDTO {

  private String join_program_invitation_id;
  private String institute;
  private String affiliate_pi_first_name;
  private String affiliate_pi_last_name;
  private String department;
}
