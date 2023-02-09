package org.icgc.argo.program_service.model.dto.builder;

import java.util.HashSet;
import java.util.Set;
import org.icgc.argo.program_service.model.dto.InstitutionDTO;
import org.icgc.argo.program_service.model.join.ProgramInstitution;
import org.springframework.stereotype.Component;

@Component
public class InstitutionDTOBuilder {
  public Set<InstitutionDTO> convertEntityToDTO(Set<ProgramInstitution> programInstitutionSet) {
    Set<InstitutionDTO> institutionDTOSet = new HashSet<>();
    for (ProgramInstitution institution : programInstitutionSet) {
      InstitutionDTO institutionDTO = new InstitutionDTO();
      institutionDTO.setName(institution.getInstitution().getName());
      institutionDTO.setId(institution.getId().getInstitutionId());
      institutionDTOSet.add(institutionDTO);
    }
    return institutionDTOSet;
  }
}
