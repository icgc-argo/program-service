package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EgoGroup {
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  private UUID id;

  @JsonProperty private String name;
  @JsonProperty private String description;
  @JsonProperty private String status;
}
