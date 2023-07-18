package org.icgc.argo.program_service.controller;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.icgc.argo.program_service.converter.Grpc2JsonConverter;
import org.icgc.argo.program_service.model.dto.CreateProgramRequestDTO;
import org.icgc.argo.program_service.model.dto.CreateProgramResponseDTO;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.CreateProgramResponse;
import org.icgc.argo.program_service.services.ProgramServiceFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/program")
public class ProgramController {
  @Autowired private ProgramServiceFacade serviceFacade;
  @Autowired private Grpc2JsonConverter grpc2JsonConverter;

  @PostMapping(value = "/createProgram")
  public ResponseEntity<CreateProgramResponseDTO> createProgram(
      @RequestBody CreateProgramRequestDTO createProgramRequestDTO) throws IOException {
    CreateProgramRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(createProgramRequestDTO),
            CreateProgramRequest.class);
    CreateProgramResponse response = serviceFacade.createProgram(request);
    return new ResponseEntity(
        grpc2JsonConverter.prepareCreateProgramResponse(response), HttpStatus.CREATED);
  }

  /*@DeleteMapping(value = "/removeProgram")
  public void removeProgram(RemoveProgramRequest request) {
      authorizationService.requireDCCAdmin();
      try {
          serviceFacade.removeProgram(request);
      } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException e) {
          log.error("Exception throw in removeProgram: {}", e.getMessage());
          throw status(NOT_FOUND, getExceptionMessage(e));
      }
  }

  @PutMapping(value = "/updateProgram")
  public @ResponseBody UpdateProgramResponse updateProgram(
          @RequestBody UpdateProgramRequest request) {
      authorizationService.requireDCCAdmin();
      UpdateProgramResponse updateProgramResponse;
      try {
          updateProgramResponse = serviceFacade.updateProgram(request);
      } catch (NotFoundException | NoSuchElementException e) {
          log.error("Exception throw in updateProgram: {}", e.getMessage());
          throw status(NOT_FOUND, e.getMessage());
      }
      return updateProgramResponse;
  }

  @GetMapping(value = "/listPrograms")
  public @ResponseBody ListProgramsResponse listPrograms() {
      return serviceFacade.listPrograms(p -> authorizationService.canRead(p.getShortName()));
  }

  @GetMapping(value = "/getProgram")
  public @ResponseBody GetProgramResponse getProgram(
          @RequestBody GetProgramRequest request) {
      authorizationService.requireProgramUser(request.getShortName().getValue());
      return serviceFacade.getProgram(request);
  }

  @PostMapping(value = "/activateProgram")
  public @ResponseBody GetProgramResponse activateProgram(@RequestBody ActivateProgramRequest request) {
      authorizationService.requireDCCAdmin();
      GetProgramResponse getProgramResponse;
      try {
          getProgramResponse = serviceFacade.activateProgram(request);
      } catch (NotFoundException | NoSuchElementException e) {
          log.error("Exception throw in updateProgram: {}", e.getMessage());
          throw status(NOT_FOUND, e.getMessage());
      }
      return getProgramResponse;
  }*/
}
