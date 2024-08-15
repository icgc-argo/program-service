package org.icgc.argo.program_service.controller;

import java.util.List;
import java.util.UUID;

import lombok.val;
import org.icgc.argo.program_service.model.dto.ProgramDTO;
import org.icgc.argo.program_service.model.dto.GetJoinProgramInviteResponseDTO;
import org.icgc.argo.program_service.model.dto.builder.ProgramDTOBuilder;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ProgramServiceFacade;
import org.icgc.argo.program_service.converter.Grpc2JsonConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/programs")
public class PublicProgramController {

  @Autowired ProgramService programService;

  @Autowired ProgramDTOBuilder programDTOBuilder;
  @Autowired private ProgramServiceFacade serviceFacade;
  @Autowired private Grpc2JsonConverter grpc2JsonConverter;

  @GetMapping(value = "/program")
  public ResponseEntity<Object> getPublicProgramData(@RequestParam(required = true) String name) {
    try {
      ProgramEntity programEntity = programService.getProgram(name, true);
      ProgramDTO programDTO = programDTOBuilder.convertEntityToDTO(programEntity);
      return new ResponseEntity(programDTO, HttpStatus.OK);
    } catch (Exception e) {
      if (e.getMessage().contains("NOT_FOUND")) {
        return new ResponseEntity(
            "Program '" + name + "' is not found, please enter a valid Program name",
            HttpStatus.NOT_FOUND);
      } else {
        return new ResponseEntity(
            "Error occurred while fetching the Program Details", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
  }

  @GetMapping(value = "/programs")
  public ResponseEntity<Object> getRegisteredProgramData() {
    List<String> programNames = programService.getAllProgramNames();
    return ResponseEntity.ok(programNames);
  }

  @GetMapping(value = "/joinProgramInvite/{invite_id}")
  public ResponseEntity<GetJoinProgramInviteResponseDTO> getJoinProgramInvite(
          @PathVariable(value = "invite_id", required = true) String inviteId) {
    val invitation = serviceFacade.getInvitationById(UUID.fromString(inviteId));
    GetJoinProgramInviteResponseDTO getJoinProgramInviteResponseDTO =
            new GetJoinProgramInviteResponseDTO();
    getJoinProgramInviteResponseDTO.setInvitation(
            grpc2JsonConverter.prepareGetJoinProgramInviteResponse(invitation));
    return new ResponseEntity<GetJoinProgramInviteResponseDTO>(
            getJoinProgramInviteResponseDTO, HttpStatus.OK);
  }
}
