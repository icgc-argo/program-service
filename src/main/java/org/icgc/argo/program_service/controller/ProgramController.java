package org.icgc.argo.program_service.controller;

import com.google.protobuf.StringValue;
import io.grpc.StatusRuntimeException;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
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
import org.springframework.dao.DataIntegrityViolationException;
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

  @PostMapping(value = "/users")
  public ResponseEntity<InviteUserResponseDTO> inviteUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody InviteUserRequestDTO inviteUserRequestDTO)
      throws IOException {

    authorizationService.requireProgramAdmin(
        inviteUserRequestDTO.getProgramShortName(), authorization);
    InviteUserRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(inviteUserRequestDTO), InviteUserRequest.class);
    return new ResponseEntity<>(
        grpc2JsonConverter.prepareInviteUserResponse(serviceFacade.inviteUser(request)),
        HttpStatus.OK);
  }

  @PostMapping(value = "/join")
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
              request, (i) -> authorizationService.requireEmail(i.getUserEmail(), authorization));

      return new ResponseEntity<>(
          grpc2JsonConverter.prepareJoinProgramResponse(response), HttpStatus.OK);
    } catch (NotFoundException e) {
      log.error("Exception throw in joinProgram: {}", e.getMessage());
      return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping(value = "/users/{shortName}")
  public ResponseEntity<List<UserDetailsDTO>> listUsers(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "shortName", required = true) String shortName) {
    authorizationService.requireProgramAdmin(shortName, authorization);
    val users = serviceFacade.listUsers(shortName);
    if (users != null && !users.getUserDetailsList().isEmpty()) {
      return new ResponseEntity<>(
          grpc2JsonConverter.prepareListUsersResponse(users).getUserDetails(), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }
  }

  @DeleteMapping(value = "/users")
  public ResponseEntity<RemoveUserResponseDTO> removeUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody RemoveUserRequestDTO removeUserRequestDTO)
      throws IOException {
    authorizationService.requireProgramAdmin(
        removeUserRequestDTO.getProgramShortName(), authorization);
    RemoveUserRequest request =
        grpc2JsonConverter.fromJson(
            grpc2JsonConverter.getJsonFromObject(removeUserRequestDTO), RemoveUserRequest.class);
    val users = serviceFacade.removeUser(request);
    if (users != null) {
      return new ResponseEntity<>(
          grpc2JsonConverter.prepareRemoveUserResponse(users), HttpStatus.OK);
    } else {
      return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
    }
  }

  @PutMapping(value = "/users")
  public void updateUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody UpdateUserRequestDTO updateUserRequestDTO) {
    authorizationService.requireProgramAdmin(updateUserRequestDTO.getShortName(), authorization);
    UpdateUserRequest request;
    try {
      request =
          grpc2JsonConverter.fromJson(
              grpc2JsonConverter.getJsonFromObject(updateUserRequestDTO), UpdateUserRequest.class);
      serviceFacade.updateUser(request);
    } catch (NotFoundException | IOException e) {
      log.error("Exception throw in joinProgram: {}", e.getMessage());
      throw new NotFoundException("User not found");
    }
  }

  @GetMapping(value = "/joinProgramInvite/{invite_id}")
  public ResponseEntity<GetJoinProgramInviteResponseDTO> getJoinProgramInvite(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "invite_id", required = true) String inviteId) {
    val invitation =
        serviceFacade.getInvitationById(
                UUID.fromString(inviteId));
    GetJoinProgramInviteResponseDTO getJoinProgramInviteResponseDTO =
        new GetJoinProgramInviteResponseDTO();
    getJoinProgramInviteResponseDTO.setInvitation(
        grpc2JsonConverter.prepareGetJoinProgramInviteResponse(invitation));
    return new ResponseEntity<GetJoinProgramInviteResponseDTO>(
        getJoinProgramInviteResponseDTO, HttpStatus.OK);
  }

  @GetMapping(value = "/cancers")
  public ResponseEntity<List<CancerDTO>> listCancers(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val listCancersResponse = serviceFacade.listCancers();
    return new ResponseEntity(
        grpc2JsonConverter.prepareListCancersResponse(listCancersResponse).getCancers(),
        HttpStatus.OK);
  }

  @GetMapping(value = "/primarySites")
  public ResponseEntity<List<PrimarySiteDTO>> listPrimarySites(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val listPrimarySitesResponse = serviceFacade.listPrimarySites();
    return new ResponseEntity(
        grpc2JsonConverter
            .prepareListPrimarySitesResponse(listPrimarySitesResponse)
            .getPrimarySites(),
        HttpStatus.OK);
  }

  @GetMapping(value = "/countries")
  public ResponseEntity<List<CountryDTO>> listCountries(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val listCountriesResponse = serviceFacade.listCountries();
    return new ResponseEntity(
        grpc2JsonConverter.prepareListCountriesResponse(listCountriesResponse).getCountries(),
        HttpStatus.OK);
  }

  @GetMapping(value = "/regions")
  public ResponseEntity<List<RegionDTO>> listRegions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val listRegionsResponse = serviceFacade.listRegions();
    return new ResponseEntity(
        grpc2JsonConverter.prepareListRegionsResponse(listRegionsResponse).getRegions(),
        HttpStatus.OK);
  }

  @GetMapping(value = "/institutions")
  public ResponseEntity<List<InstitutionDTO>> listInstitutions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val listInstitutionsResponse = serviceFacade.listInstitutions();
    return new ResponseEntity(
        grpc2JsonConverter
            .prepareListInstitutionsResponse(listInstitutionsResponse)
            .getInstitutions(),
        HttpStatus.OK);
  }

  @PostMapping(value = "/institutions")
  public ResponseEntity<AddInstitutionsResponseDTO> addInstitutions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody AddInstitutionsRequestDTO addInstitutionsRequestDTO) {

    AddInstitutionsResponseDTO addInstitutionsResponseDTO = null;
    try {
      val response = serviceFacade.addInstitutions(addInstitutionsRequestDTO.getNames());
      addInstitutionsResponseDTO = grpc2JsonConverter.prepareAddInstitutionsResponse(response);
    } catch (DataIntegrityViolationException e) {
      log.error("Exception throw in addInstitutions: {}", e.getMessage());
      return new ResponseEntity(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity(addInstitutionsResponseDTO, HttpStatus.OK);
  }
}
