package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EgoCollection<T> {
  @JsonProperty List<T> resultSet;

  @JsonProperty Integer limit;

  @JsonProperty Integer offset;
}
