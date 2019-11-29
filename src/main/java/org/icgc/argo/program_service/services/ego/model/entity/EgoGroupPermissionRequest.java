package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EgoGroupPermissionRequest {
  @JsonProperty private String groupName;
  @JsonProperty private String policyName;
  @JsonProperty private String mask;
}
