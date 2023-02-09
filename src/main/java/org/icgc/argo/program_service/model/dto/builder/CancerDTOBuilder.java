package org.icgc.argo.program_service.model.dto.builder;

import java.util.HashSet;
import java.util.Set;
import org.icgc.argo.program_service.model.dto.CancerDTO;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.springframework.stereotype.Component;

@Component
public class CancerDTOBuilder {
  public Set<CancerDTO> convertEntityToDTO(Set<ProgramCancer> programCancerSet) {
    Set<CancerDTO> cancerDTOSet = new HashSet<>();
    for (ProgramCancer cancer : programCancerSet) {
      CancerDTO cancerDTO = new CancerDTO();
      cancerDTO.setName(cancer.getCancer().getName());
      cancerDTO.setId(cancer.getCancer().getId());
      cancerDTOSet.add(cancerDTO);
    }
    return cancerDTOSet;
  }
}
