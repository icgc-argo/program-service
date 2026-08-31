package org.icgc.argo.program_service.model.dto.builder;

import java.util.UUID;
import org.icgc.argo.program_service.model.dto.DataCenterDetailsDTO;
import org.icgc.argo.program_service.model.dto.ProgramDTO;
import org.icgc.argo.program_service.model.entity.DataCenterEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.services.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProgramDTOBuilder {

  @Autowired CancerDTOBuilder canerDtoBuilder;
  @Autowired CountryDTOBuilder countryDTOBuilder;
  @Autowired InstitutionDTOBuilder institutionDTOBuilder;
  @Autowired PrimarySiteDTOBuilder primarySiteDTOBuilder;
  @Autowired RegionDTOBuilder regionDTOBuilder;
  @Autowired ProgramService programService;

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

    UUID dataCenterId = programEntity.getDataCenterId();
    if (dataCenterId != null) {
      DataCenterEntity dataCenterEntity = programService.getDataCenterDetails(dataCenterId);
      programDTO.setDataCenter(
          DataCenterDetailsDTO.builder()
              .id(String.valueOf(dataCenterEntity.getId()))
              .shortName(dataCenterEntity.getShortName())
              .name(dataCenterEntity.getName())
              .uiUrl(dataCenterEntity.getUiUrl())
              .gatewayUrl(dataCenterEntity.getGatewayUrl())
              .build());
    }

    return programDTO;
  }
}
