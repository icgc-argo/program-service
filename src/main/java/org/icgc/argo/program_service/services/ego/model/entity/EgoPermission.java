package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EgoPermission {
  @JsonProperty private String accessLevel;
  @JsonProperty private UUID id;
  EgoPolicy policy;
  // etc
}
