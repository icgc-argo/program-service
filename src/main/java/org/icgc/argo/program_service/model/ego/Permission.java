package org.icgc.argo.program_service.model.ego;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor @Data
public class Permission {
  @JsonProperty
  private String accessLevel;
  // etc
}
