/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.icgc.argo.program_service.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.User;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.icgc.argo.program_service.repositories.query.ProgramSpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.HttpClientErrorException;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOError;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
public class ProgramService {

  /**
   * Dependencies
   */
  private final JoinProgramInviteRepository invitationRepository;
  private final ProgramRepository programRepository;
  private final ProgramConverter programConverter;
  private final EgoService egoService;
  private final MailService mailService;

  @Autowired
  public ProgramService(@NonNull JoinProgramInviteRepository invitationRepository,
      @NonNull ProgramRepository programRepository,
      @NonNull ProgramConverter programConverter,
      @NonNull EgoService egoService,
      @NonNull MailService mailService
  ) {
    this.invitationRepository = invitationRepository;
    this.programRepository = programRepository;
    this.egoService = egoService;
    this.programConverter = programConverter;
    this.mailService = mailService;
  }

  public Optional<ProgramEntity> getProgram(@NonNull String name) {
    return programRepository.findByName(name);
  }

  public Optional<ProgramEntity> getProgram(@NonNull UUID uuid) {
    return programRepository.findById(uuid);
  }

  public ProgramEntity createProgram(@NonNull Program program) throws DataIntegrityViolationException {
    val programEntity = programConverter.programToProgramEntity(program);

    // Set the timestamps
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    programRepository.save(programEntity);
    egoService.setUpProgram(programEntity);
    return programEntity;
  }

  public void removeProgram(@NonNull ProgramEntity program) {
    egoService.cleanUpProgram(program);
    programRepository.deleteById(program.getId());
  }

  public void removeProgram(UUID programId) throws EmptyResultDataAccessException {
    programRepository.deleteById(programId);
  }

  public List<ProgramEntity> listPrograms() {
    return programRepository.findAll(new ProgramSpecificationBuilder()
        .setFetchCancers(true)
        .setFetchPrimarySites(true)
        .listAll());
  }

  public List<User> listUser(UUID programId){
    val users = egoService.getUserByGroup(programId);
    return users.collect(Collectors.toList());
  }

  public UUID inviteUser(@NotNull ProgramEntity program,
                         @Email @NotNull String userEmail,
                         @NotBlank @NotNull String firstName,
                         @NotBlank @NotNull String lastName,
                         @NotNull UserRole role) {
    val invitation = new JoinProgramInvite(program, userEmail, firstName, lastName, role);
    invitationRepository.save(invitation);
    mailService.sendInviteEmail(invitation);
    return invitation.getId();
  }

  public boolean acceptInvite(UUID invitationId) {
    val invitation = invitationRepository.findById(invitationId);
    if (invitation.isPresent()) {
      val i = invitation.get();
      i.accept();
      val email = invitation.get().getUserEmail();
      egoService.joinProgram(email, i.getProgram(), i.getRole());
      invitationRepository.save(invitation.get());
      return true;
    } else {
      return false;
    }
  }
}
