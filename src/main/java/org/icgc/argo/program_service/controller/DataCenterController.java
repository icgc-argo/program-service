package org.icgc.argo.program_service.controller;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.Grpc2JsonConverter;
import org.icgc.argo.program_service.model.dto.DataCenterDTO;
import org.icgc.argo.program_service.services.ProgramServiceFacade;
import org.icgc.argo.program_service.services.auth.RestAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/datacenters")
public class DataCenterController {
  @Autowired private ProgramServiceFacade serviceFacade;
  @Autowired private Grpc2JsonConverter grpc2JsonConverter;
  @Autowired private RestAuthorizationService authorizationService;

  @GetMapping
  public ResponseEntity<List<DataCenterDTO>> listDataCenters(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val dataCenterEntities = serviceFacade.listDataCenters();
    return new ResponseEntity(dataCenterEntities, HttpStatus.OK);
  }
  
 @GetMapping(value = "/datacenters/{datacenter_short_name}/programs")
  public ResponseEntity<ProgramsResponseDTO> listDataCenterPrograms(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
      final String authorization,
      @PathVariable(value = "datacenter_short_name", required = true) String dataCenterShortName) {
    authorizationService.requireDCCAdmin(authorization);
    val listProgramsResponse = serviceFacade.listProgramsByDataCenter(dataCenterShortName);
    return new ResponseEntity(
        grpc2JsonConverter.prepareListProgramsResponse(listProgramsResponse), HttpStatus.OK);
  }
}
