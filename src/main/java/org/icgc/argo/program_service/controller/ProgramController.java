package org.icgc.argo.program_service.controller;

import com.google.protobuf.StringValue;
import io.grpc.StatusRuntimeException;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.icgc.argo.program_service.converter.Grpc2JsonConverter;
import org.icgc.argo.program_service.model.dto.*;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.ProgramServiceFacade;
import org.icgc.argo.program_service.services.auth.RestAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/programs")
public class ProgramController {
  @Autowired private ProgramServiceFacade serviceFacade;
  @Autowired private Grpc2JsonConverter grpc2JsonConverter;
  @Autowired private RestAuthorizationService authorizationService;

  @PostMapping
  public ResponseEntity<CreateProgramResponseDTO> createProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody CreateProgramRequestDTO createProgramRequestDTO)
      throws IOException {
    authorizationService.requireDCCAdmin(authorization);
    CreateProgramRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(createProgramRequestDTO),
            CreateProgramRequest.class);
    CreateProgramResponse response = serviceFacade.createProgram(request);
    return new ResponseEntity(
        grpc2JsonConverter.prepareCreateProgramResponse(response), HttpStatus.CREATED);
  }

  @DeleteMapping(value = "/{shortName}")
  public void removeProgram(
      @PathVariable(value = "shortName", required = true) String shortName,
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    authorizationService.requireDCCAdmin(authorization);
    try {
      RemoveProgramRequest request =
          RemoveProgramRequest.newBuilder().setProgramShortName(StringValue.of(shortName)).build();
      serviceFacade.removeProgram(request);
    } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException e) {
      log.error("Exception thrown in removeProgram: {}", e.getMessage());
      throw new NotFoundException(ExceptionUtils.getStackTrace(e));
    }
  }

  @PutMapping
  public ResponseEntity<UpdateProgramResponseDTO> updateProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody UpdateProgramRequestDTO updateProgramRequestDTO) {
    authorizationService.requireDCCAdmin(authorization);
    UpdateProgramRequest request;
    UpdateProgramResponse response;
    try {
      request =
          grpc2JsonConverter.fromJson(
              grpc2JsonConverter.getJsonFromObject(updateProgramRequestDTO),
              UpdateProgramRequest.class);
      response = serviceFacade.updateProgram(request);
    } catch (NotFoundException | NoSuchElementException | IOException e) {
      log.error("Exception thrown in updateProgram: {}", e.getMessage());
      return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity(
        grpc2JsonConverter.prepareUpdateProgramResponse(response), HttpStatus.OK);
  }

  @GetMapping(value = "/{shortName}")
  public ResponseEntity<GetProgramResponseDTO> getProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "shortName", required = true) String shortName)
      throws IOException {
    GetProgramResponseDTO getProgramResponseDTO = null;
    try {
      authorizationService.requireProgramUser(shortName, authorization);
      GetProgramRequest request =
          GetProgramRequest.newBuilder().setShortName(StringValue.of(shortName)).build();
      GetProgramResponse response = serviceFacade.getProgram(request);
      getProgramResponseDTO = grpc2JsonConverter.prepareGetProgramResponse(response);
    } catch (StatusRuntimeException exception) {
      if (exception.getStatus().getCode().name().equalsIgnoreCase(HttpStatus.NOT_FOUND.name()))
      log.error("Exception thrown in getProgram: {}", exception.getMessage());
      return new ResponseEntity(exception.getMessage(), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity(getProgramResponseDTO, HttpStatus.OK);
  }

  @PostMapping(value = "/activate")
  public ResponseEntity<GetProgramResponseDTO> activateProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody ActivateProgramRequestDTO activateProgramRequestDTO) {
    authorizationService.requireDCCAdmin(authorization);
    GetProgramResponse response;
    ActivateProgramRequest request;
    try {
      request =
          grpc2JsonConverter.fromJson(
              grpc2JsonConverter.getJsonFromObject(activateProgramRequestDTO),
              ActivateProgramRequest.class);
      response = serviceFacade.activateProgram(request);
    } catch (NotFoundException | NoSuchElementException | IOException e) {
      log.error("Exception throw in activateProgram: {}", e.getMessage());
      throw new NotFoundException(ExceptionUtils.getStackTrace(e));
    }
    return new ResponseEntity(
        grpc2JsonConverter.prepareGetProgramResponse(response), HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<List<ProgramsResponseDTO>> listPrograms(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val listProgramsResponse =
        serviceFacade.listPrograms(
            p -> authorizationService.canRead(p.getShortName(), authorization));
    return new ResponseEntity(
        grpc2JsonConverter.prepareListProgramsResponse(listProgramsResponse), HttpStatus.OK);
  }
}
