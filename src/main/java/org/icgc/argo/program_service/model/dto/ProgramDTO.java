package org.icgc.argo.program_service.model.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProgramDTO {

  private String shortName;

  private String description;

  private String name;

  private String website;

  private Set<InstitutionDTO> programInstitutions;

  private Set<CountryDTO> programCountries;

  private Set<RegionDTO> processingRegions;

  private Set<CancerDTO> programCancers;

  private Set<PrimarySiteDTO> programPrimarySites;
}
