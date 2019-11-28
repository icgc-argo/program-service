package org.icgc.argo.program_service.services.ego.model.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EgoMassDeleteRequest {
  List<String> policyNames;
  List<String> groupNames;
}
