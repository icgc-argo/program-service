package org.icgc.argo.program_service.model.dto;

import java.util.UUID;
import lombok.*;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CancerDTO {
  private UUID id;
  private String name;
}
