package org.icgc.argo.program_service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.icgc.argo.program_service.proto.MembershipTypeValue;

import java.util.List;
import java.util.Set;

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

  private List<String> regions;

  private List<String> cancerTypes;

  private List<String> primarySites;

}
