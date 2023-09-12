package org.icgc.argo.program_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailsDTO {

  private UserDTO user;
  private InviteStatusValueDTO status;
  private String acceptedAt;
  private boolean dacoApproved;
}
