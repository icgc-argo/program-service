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
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.User;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

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
  private final MailSender mailSender;
  private final EgoService egoService;
  private final MailService mailService;
  private final CommonConverter commonConverter;

  @Autowired
  public ProgramService (
      @NonNull JoinProgramInviteRepository invitationRepository,
      @NonNull ProgramRepository programRepository,
      @NonNull ProgramConverter programConverter,
      @NonNull MailService mailService,
      @NonNull MailSender mailSender,
      @NonNull EgoService egoService,
      @NonNull CommonConverter commonConverter) {
    this.invitationRepository = invitationRepository;
    this.programRepository = programRepository;
    this.programConverter = programConverter;
    this.mailService = mailService;
    this.mailSender = mailSender;
    this.egoService = egoService;
    this.commonConverter = commonConverter;
  }

  public Optional<ProgramEntity> getProgram(@NonNull String name) {
    return programRepository.findByShortName(name);
  }

  public Optional<ProgramEntity> getProgram(@NonNull UUID uuid) {
    return programRepository.findById(uuid);
  }

  //TODO: add existence check, and ensure program doesnt already exist. If it does, return a Conflict
  public ProgramEntity createProgram(@NonNull Program program, @NonNull Collection<String> adminEmails) throws DataIntegrityViolationException {
    val programEntity = programConverter.programToProgramEntity(program);

    // Set the timestamps
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);
    programRepository.save(programEntity);
    return programEntity;
  }

  public ProgramEntity updateProgram(@NonNull Program program) {
    val programToUpdate = programRepository
            .findByShortName(program.getShortName().toString())
            .orElseThrow(
                    () -> new NotFoundException(String.format("The program with short name: {} is not found.",
                                                      commonConverter.unboxStringValue(program.getShortName()))));

    val updatingProgram = programConverter.programToProgramEntity(program);

    // update basic info program
    programConverter.updateProgram(updatingProgram, programToUpdate);
    programRepository.save(programToUpdate);
    return programToUpdate;
  }

  //TODO: add existence check, and fail with not found
  public void removeProgram(@NonNull ProgramEntity program) {
    egoService.cleanUpProgram(program);
    programRepository.deleteById(program.getId());
  }

  public void removeProgram(UUID programId) throws EmptyResultDataAccessException {
    programRepository.deleteById(programId);
  }

  public List<ProgramEntity> listPrograms() {
    return programRepository.findAll();
  }

  public List<User> listUser(@NonNull UUID programId){
    return egoService.getUserByGroup(programId);
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
