package org.icgc.argo.program_service.model.ego;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.UUID;

@Accessors(chain = true)
@AllArgsConstructor @NoArgsConstructor @Data
public class User {
  @JsonProperty
  private UUID id;

  @JsonProperty
  private String email;

  @JsonProperty
  private String firstName;

  @JsonProperty
  private String type;

  @JsonProperty
  private String status;

  @JsonProperty
  private String lastName;
}
