package org.icgc.argo.program_service.model.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateProgramRequestDTO {

  private Program program;
  private UUID dataCenterId;
  List<UserDTO> admins;
}
