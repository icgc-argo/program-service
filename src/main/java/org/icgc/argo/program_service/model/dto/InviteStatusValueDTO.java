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
public class InviteStatusValueDTO {

  private InviteStatus value;
}
