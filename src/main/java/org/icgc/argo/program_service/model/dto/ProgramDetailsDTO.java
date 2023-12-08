package org.icgc.argo.program_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgramDetailsDTO {

  private ProgramsDTO program;
  private MetadataDTO metadata;
  private LegacyDetailsDTO legacy;
}
