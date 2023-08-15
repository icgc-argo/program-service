package org.icgc.argo.program_service.controller;

import com.google.protobuf.StringValue;
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
import org.icgc.argo.program_service.services.auth.AuthorizationService;
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
  @Autowired private AuthorizationService authorizationService;

  @PostMapping
  public ResponseEntity<CreateProgramResponseDTO> createProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody CreateProgramRequestDTO createProgramRequestDTO)
      throws IOException {
    authorizationService.requireDCCAdmin();
    CreateProgramRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(createProgramRequestDTO),
            CreateProgramRequest.class);
    CreateProgramResponse response = serviceFacade.createProgram(request);
    return new ResponseEntity(
        grpc2JsonConverter.prepareCreateProgramResponse(response), HttpStatus.CREATED);
  }

  @DeleteMapping(value = "/{programShortName}")
  public void removeProgram(
      @PathVariable(value = "shortName", required = true) String shortName,
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    authorizationService.requireDCCAdmin();
    try {
      RemoveProgramRequest request =
          RemoveProgramRequest.newBuilder().setProgramShortName(StringValue.of(shortName)).build();
      serviceFacade.removeProgram(request);
    } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException e) {
      log.error("Exception throw in removeProgram: {}", e.getMessage());
      throw new NotFoundException(ExceptionUtils.getStackTrace(e));
    }
  }

  @PutMapping
  public ResponseEntity<UpdateProgramResponseDTO> updateProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
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

  @GetMapping(value = "/{shortName}")
  public ResponseEntity<GetProgramResponseDTO> getProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "shortName", required = true) String shortName)
      throws IOException {
    authorizationService.requireProgramUser(shortName);
    GetProgramRequest request =
        GetProgramRequest.newBuilder().setShortName(StringValue.of(shortName)).build();
    GetProgramResponse response = serviceFacade.getProgram(request);
    return new ResponseEntity(
        grpc2JsonConverter.prepareGetProgramResponse(response), HttpStatus.OK);
  }

  @PostMapping(value = "/activate")
  public ResponseEntity<GetProgramResponseDTO> activateProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
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

  @GetMapping
  public ResponseEntity<List<ProgramsResponseDTO>> listPrograms(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val listProgramsResponse =
        serviceFacade.listPrograms(p -> authorizationService.canRead(p.getShortName()));
    return new ResponseEntity(
        grpc2JsonConverter.prepareListProgramsResponse(listProgramsResponse), HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<InviteUserResponseDTO> inviteUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody InviteUserRequestDTO inviteUserRequestDTO)
      throws IOException {

    authorizationService.requireProgramAdmin(inviteUserRequestDTO.getProgramShortName());
    InviteUserRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(inviteUserRequestDTO), InviteUserRequest.class);
    return new ResponseEntity<>(
        grpc2JsonConverter.prepareInviteUserResponse(serviceFacade.inviteUser(request)),
        HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<JoinProgramResponseDTO> joinProgram(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody JoinProgramRequestDTO joinProgramRequestDTO)
      throws IOException {
    try {
      JoinProgramRequest request =
          grpc2JsonConverter.fromJson(
              grpc2JsonConverter.getJsonFromObject(joinProgramRequestDTO),
              JoinProgramRequest.class);
      val response =
          serviceFacade.joinProgram(
              request, (i) -> authorizationService.requireEmail(i.getUserEmail()));

      return new ResponseEntity<>(
          grpc2JsonConverter.prepareJoinProgramResponse(response), HttpStatus.OK);
    } catch (NotFoundException e) {
      log.error("Exception throw in joinProgram: {}", e.getMessage());
      return new ResponseEntity<>(new JoinProgramResponseDTO(), HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping
  public ResponseEntity<List<UserDetailsDTO>> listUsers(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "shortName", required = true) String shortName) {
    authorizationService.requireProgramAdmin(shortName);
    return new ResponseEntity<>(
        grpc2JsonConverter
            .prepareListUsersResponse(serviceFacade.listUsers(shortName))
            .getUserDetails(),
        HttpStatus.OK);
  }

  @DeleteMapping
  public ResponseEntity<RemoveUserResponseDTO> removeUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody RemoveUserRequestDTO removeUserRequestDTO)
      throws IOException {
    authorizationService.requireProgramAdmin(removeUserRequestDTO.getProgramShortName());
    RemoveUserRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(removeUserRequestDTO), RemoveUserRequest.class);
    return new ResponseEntity<>(
        grpc2JsonConverter.prepareRemoveUserResponse(serviceFacade.removeUser(request)),
        HttpStatus.OK);
  }

  @PutMapping
  public void updateUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody UpdateUserRequestDTO updateUserRequestDTO) {
    authorizationService.requireProgramAdmin(updateUserRequestDTO.getShortName());
    UpdateUserRequest request;
    try {
      request =
          grpc2JsonConverter.fromJson(
              grpc2JsonConverter.getJsonFromObject(updateUserRequestDTO), UpdateUserRequest.class);
      serviceFacade.updateUser(request);
    } catch (NotFoundException | IOException e) {
      log.error("Exception throw in joinProgram: {}", e.getMessage());
    }
  }
}
