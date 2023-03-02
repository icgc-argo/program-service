package org.icgc.argo.program_service.model.dto.builder;

import java.util.HashSet;
import java.util.Set;
import org.icgc.argo.program_service.model.dto.PrimarySiteDTO;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.springframework.stereotype.Component;

@Component
public class PrimarySiteDTOBuilder {

  public Set<PrimarySiteDTO> convertEntityToDTO(Set<ProgramPrimarySite> programPrimarySiteSet) {
    Set<PrimarySiteDTO> primarySiteDTOSet = new HashSet<>();
    for (ProgramPrimarySite primarySite : programPrimarySiteSet) {
      PrimarySiteDTO primarySiteDTO = new PrimarySiteDTO();
      primarySiteDTO.setName(primarySite.getPrimarySite().getName());
      primarySiteDTO.setId(primarySite.getId().getPrimarySiteId());
      primarySiteDTOSet.add(primarySiteDTO);
    }
    return primarySiteDTOSet;
  }
}
