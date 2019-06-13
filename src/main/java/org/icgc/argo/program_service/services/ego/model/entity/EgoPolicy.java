package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor @Data
public class EgoPolicy {
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private UUID id;
  @JsonProperty()
  private String name;
}