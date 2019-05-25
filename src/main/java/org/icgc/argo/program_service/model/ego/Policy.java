package org.icgc.argo.program_service.model.ego;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor @Data
public class Policy {
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private UUID id;
  @JsonProperty()
  private String name;
}
