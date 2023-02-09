package org.icgc.argo.program_service.model.dto.builder;

import java.util.HashSet;
import java.util.Set;
import org.icgc.argo.program_service.model.dto.RegionDTO;
import org.icgc.argo.program_service.model.join.ProgramRegion;
import org.springframework.stereotype.Component;

@Component
public class RegionDTOBuilder {
  public Set<RegionDTO> convertEntityToDTO(Set<ProgramRegion> programRegionSet) {
    Set<RegionDTO> regionDTOSet = new HashSet<>();
    for (ProgramRegion region : programRegionSet) {
      RegionDTO regionDTO = new RegionDTO();
      regionDTO.setName(region.getRegion().getName());
      regionDTO.setId(region.getRegion().getId());
      regionDTOSet.add(regionDTO);
    }
    return regionDTOSet;
  }
}
