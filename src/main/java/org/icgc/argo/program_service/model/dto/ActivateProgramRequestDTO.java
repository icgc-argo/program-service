package org.icgc.argo.program_service.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivateProgramRequestDTO {

  private String original_short_name;
  private String updated_short_name;
  private List<UserDTO> admins;
}
