package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor @Data
public class EgoPermission {
  @JsonProperty
  private String accessLevel;
  @JsonProperty
  private UUID id;
  EgoPolicy policy;
  // etc
}
