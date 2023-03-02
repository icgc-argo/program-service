package org.icgc.argo.program_service.model.dto.builder;

import java.util.HashSet;
import java.util.Set;
import org.icgc.argo.program_service.model.dto.CountryDTO;
import org.icgc.argo.program_service.model.join.ProgramCountry;
import org.springframework.stereotype.Component;

@Component
public class CountryDTOBuilder {
  public Set<CountryDTO> convertEntityToDTO(Set<ProgramCountry> programCountrySet) {
    Set<CountryDTO> countryDTOSet = new HashSet<>();
    for (ProgramCountry country : programCountrySet) {
      CountryDTO countryDTO = new CountryDTO();
      countryDTO.setName(country.getCountry().getName());
      countryDTO.setId(country.getId().getCountryId());
      countryDTOSet.add(countryDTO);
    }
    return countryDTOSet;
  }
}
