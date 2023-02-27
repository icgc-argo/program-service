package org.icgc.argo.program_service.controller;

import java.util.List;
import org.icgc.argo.program_service.model.dto.ProgramDTO;
import org.icgc.argo.program_service.model.dto.builder.ProgramDTOBuilder;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.services.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
public class PublicProgramController {

  @Autowired ProgramService programService;

  @Autowired ProgramDTOBuilder programDTOBuilder;

  @GetMapping(value = "/program")
  public ResponseEntity<ProgramDTO> getPublicProgramData(
      @RequestParam(required = true) String name) {

    ProgramEntity programEntity = programService.getProgram(name, true);

    ProgramDTO programDTO = programDTOBuilder.convertEntityToDTO(programEntity);

    return ResponseEntity.ok(programDTO);
  }

  @GetMapping(value = "/registeredPrograms")
  public ResponseEntity<Object> getRegisteredProgramData() {
    List<String> programNames = programService.getAllProgramNames();
    return ResponseEntity.ok(programNames);
  }
}
