package org.icgc.argo.program_service.controller;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.Grpc2JsonConverter;
import org.icgc.argo.program_service.model.dto.DataCenterDTO;
import org.icgc.argo.program_service.model.dto.DataCenterRequestDTO;
import org.icgc.argo.program_service.model.dto.ProgramsResponseDTO;
import org.icgc.argo.program_service.services.ProgramServiceFacade;
import org.icgc.argo.program_service.services.auth.RestAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    return new ResponseEntity(
        serviceFacade.listProgramsByDataCenter(dataCenterShortName), HttpStatus.OK);
  }

  @PostMapping(value = "/datacenters")
  public ResponseEntity<DataCenterDTO> createDataCenter(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody DataCenterRequestDTO dataCenterRequestDTO) {
    authorizationService.requireDCCAdmin(authorization);
    val dataCenterEntity = serviceFacade.createDataCenter(dataCenterRequestDTO);
    return new ResponseEntity(dataCenterEntity, HttpStatus.OK);
  }

  @PatchMapping(value = "/datacenters/{datacenter_short_name}")
  public ResponseEntity<DataCenterDTO> updateDataCenter(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "datacenter_short_name", required = true) String dataCenterShortName,
      @RequestBody DataCenterRequestDTO dataCenterRequestDTO) {
    authorizationService.requireDCCAdmin(authorization);
    val dataCenterEntity =
        serviceFacade.updateDataCenter(dataCenterShortName, dataCenterRequestDTO);
    return new ResponseEntity(dataCenterEntity, HttpStatus.OK);
  }
}
