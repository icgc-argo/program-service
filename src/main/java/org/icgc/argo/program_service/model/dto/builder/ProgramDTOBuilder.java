package org.icgc.argo.program_service.model.dto.builder;

import org.icgc.argo.program_service.model.dto.ProgramDTO;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProgramDTOBuilder {

  @Autowired CancerDTOBuilder canerDtoBuilder;
  @Autowired CountryDTOBuilder countryDTOBuilder;
  @Autowired InstitutionDTOBuilder institutionDTOBuilder;
  @Autowired PrimarySiteDTOBuilder primarySiteDTOBuilder;
  @Autowired RegionDTOBuilder regionDTOBuilder;

  public ProgramDTO convertEntityToDTO(ProgramEntity programEntity) {
    ProgramDTO programDTO = new ProgramDTO();
    programDTO.setName(programEntity.getName());
    programDTO.setWebsite(programEntity.getWebsite());
    programDTO.setShortName(programEntity.getShortName());
    programDTO.setDescription(programEntity.getDescription());
    programDTO.setProgramCancers(
        canerDtoBuilder.convertEntityToDTO(programEntity.getProgramCancers()));
    programDTO.setProgramCountries(
        countryDTOBuilder.convertEntityToDTO(programEntity.getProgramCountries()));
    programDTO.setProgramPrimarySites(
        primarySiteDTOBuilder.convertEntityToDTO(programEntity.getProgramPrimarySites()));
    programDTO.setProgramInstitutions(
        institutionDTOBuilder.convertEntityToDTO(programEntity.getProgramInstitutions()));
    return programDTO;
  }
}
