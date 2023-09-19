package org.icgc.argo.program_service.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProgramsDTO {

  private String shortName;

  private String description;

  private String name;

  private String website;

  private List<String> institutions;

  private List<String> countries;

  private List<String> cancerTypes;

  private List<String> primarySites;
}
