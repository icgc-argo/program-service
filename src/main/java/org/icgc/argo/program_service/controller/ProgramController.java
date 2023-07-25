package org.icgc.argo.program_service.controller;

import com.google.protobuf.StringValue;
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
import org.icgc.argo.program_service.services.auth.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/program")
public class ProgramController {

  @Autowired private ProgramServiceFacade serviceFacade;
  @Autowired private Grpc2JsonConverter grpc2JsonConverter;
  @Autowired private AuthorizationService authorizationService;

  @PostMapping(value = "/createProgram")
  public ResponseEntity<CreateProgramResponseDTO> createProgram(
      @RequestBody CreateProgramRequestDTO createProgramRequestDTO) throws IOException {
    authorizationService.requireDCCAdmin();
    CreateProgramRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(createProgramRequestDTO),
            CreateProgramRequest.class);
    CreateProgramResponse response = serviceFacade.createProgram(request);
    return new ResponseEntity(
        grpc2JsonConverter.prepareCreateProgramResponse(response), HttpStatus.CREATED);
  }

  @DeleteMapping(value = "/removeProgram")
  public void removeProgram(@RequestBody RemoveProgramRequestDTO removeProgramRequestDTO) {
    authorizationService.requireDCCAdmin();
    RemoveProgramRequest request;
    try {
      request =
          grpc2JsonConverter.fromJson(
              grpc2JsonConverter.getJsonFromObject(removeProgramRequestDTO),
              RemoveProgramRequest.class);
      serviceFacade.removeProgram(request);
    } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException | IOException e) {
      log.error("Exception throw in removeProgram: {}", e.getMessage());
      throw new NotFoundException(ExceptionUtils.getStackTrace(e));
    }
  }

  @PutMapping(value = "/updateProgram")
  public ResponseEntity<UpdateProgramResponseDTO> updateProgram(
      @RequestBody UpdateProgramRequestDTO updateProgramRequestDTO) {
    authorizationService.requireDCCAdmin();
    UpdateProgramRequest request;
    UpdateProgramResponse response;
    try {
      request =
          grpc2JsonConverter.fromJson(
              grpc2JsonConverter.getJsonFromObject(updateProgramRequestDTO),
              UpdateProgramRequest.class);
      response = serviceFacade.updateProgram(request);
    } catch (NotFoundException | NoSuchElementException | IOException e) {
      log.error("Exception throw in updateProgram: {}", e.getMessage());
      throw new NotFoundException(ExceptionUtils.getStackTrace(e));
    }
    return new ResponseEntity(
        grpc2JsonConverter.prepareUpdateProgramResponse(response), HttpStatus.OK);
  }

  @GetMapping(value = "/getProgram/{shortName}")
  public ResponseEntity<GetProgramResponseDTO> getProgram(
      @PathVariable(value = "shortName", required = true) String shortName) throws IOException {
    authorizationService.requireProgramUser(shortName);
    GetProgramRequest request =
        GetProgramRequest.newBuilder().setShortName(StringValue.of(shortName)).build();
    GetProgramResponse response = serviceFacade.getProgram(request);
    return new ResponseEntity(
        grpc2JsonConverter.prepareGetProgramResponse(response), HttpStatus.OK);
  }

  @PostMapping(value = "/activateProgram")
  public ResponseEntity<GetProgramResponseDTO> activateProgram(
      @RequestBody ActivateProgramRequestDTO activateProgramRequestDTO) {
    authorizationService.requireDCCAdmin();
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

  @GetMapping(value = "/listProgram")
  public ResponseEntity<List<ProgramsResponseDTO>> listPrograms() {
    val listProgramsResponse =
        serviceFacade.listPrograms(p -> authorizationService.canRead(p.getShortName()));
    return new ResponseEntity(
        grpc2JsonConverter.prepareListProgramsResponse(listProgramsResponse), HttpStatus.OK);
  }
}
