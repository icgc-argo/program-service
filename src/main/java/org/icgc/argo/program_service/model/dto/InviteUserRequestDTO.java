package org.icgc.argo.program_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InviteUserRequestDTO {

  private String programShortName;
  private String firstName;
  private String lastName;
  private String email;
  private UserRoleValueDTO role;
}
