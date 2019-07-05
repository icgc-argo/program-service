package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.icgc.argo.program_service.proto.UserRole;
import java.util.UUID;

@Accessors(chain = true)
@Builder
@AllArgsConstructor @NoArgsConstructor @Data
public class EgoUser {

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

  private UserRole role;

}
