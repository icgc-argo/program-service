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

import io.grpc.Status;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.User;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.CancerRepository;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.PrimarySiteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.icgc.argo.program_service.repositories.query.ProgramSpecificationBuilder;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.icgc.argo.program_service.utils.CollectionUtils.*;
import static org.icgc.argo.program_service.utils.EntityService.getManyEntities;

@Service
@Validated
@Slf4j
public class ProgramService {

  /**
   * Dependencies
   */
  private final JoinProgramInviteRepository invitationRepository;
  private final ProgramRepository programRepository;
  private final CancerRepository cancerRepository;
  private final PrimarySiteRepository primarySiteRepository;
  private final ProgramConverter programConverter;
  private final EgoService egoService;
  private final MailService mailService;

  @Autowired
  public ProgramService (
      @NonNull JoinProgramInviteRepository invitationRepository,
      @NonNull ProgramRepository programRepository,
      @NonNull CancerRepository cancerRepository,
      @NonNull PrimarySiteRepository primarySiteRepository,
      @NonNull ProgramConverter programConverter,
      @NonNull MailService mailService,
      @NonNull MailSender mailSender,
      @NonNull EgoService egoService,
      @NonNull CommonConverter commonConverter) {
    this.invitationRepository = invitationRepository;
    this.programRepository = programRepository;
    this.cancerRepository = cancerRepository;
    this.primarySiteRepository = primarySiteRepository;
    this.programConverter = programConverter;
    this.mailService = mailService;
    this.egoService = egoService;
  }

  public ProgramEntity getProgram(@NonNull String name) {
     val program = programRepository.findByShortName(name);
     if (program.isEmpty()) {
       throw Status.NOT_FOUND.
         withDescription("Program '" + name + "' not found").
         asRuntimeException();
     }
     return program.get();
  }

  //TODO: add existence check, and ensure program doesnt already exist. If it does, return a Conflict
  public ProgramEntity createProgram(@NonNull Program program, @NonNull Collection<String> adminEmails) throws DataIntegrityViolationException {
    val programEntity = programConverter.programToProgramEntity(program);
    val id = UUID.randomUUID();
    programEntity.setId(id);

    val cancers = getCancers(program.getCancerTypesList());
    val primarySites = getPrimarySites(program.getPrimarySitesList());

    cancers.forEach(programEntity::associateCancer);
    primarySites.forEach(programEntity::associatePrimarySite);

    // Set the timestamps
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    programRepository.save(programEntity);
    return programEntity;
  }

  private List<CancerEntity> getCancers(List<String> cancerTypesList) {
    return cancerTypesList.stream().
      map(this::getCancer).
      collect(Collectors.toList());
  }

  private List<PrimarySiteEntity> getPrimarySites(List<String> primarySitesList) {
    return primarySitesList.stream().
      map(this::getPrimarySite).
      collect(Collectors.toList());
  }

  public ProgramEntity updateProgram(@NonNull ProgramEntity updatingProgram) throws NotFoundException, EmptyResultDataAccessException {
    val programToUpdate = getProgram(updatingProgram.getShortName());

    //update associations
    processCancers(programToUpdate, updatingProgram);
    processPrimarySites(programToUpdate, updatingProgram);

    // update basic info program
    programConverter.updateProgram(updatingProgram, programToUpdate);
    programRepository.save(programToUpdate);
    return programToUpdate;
  }

  private CancerEntity getCancer(String cancerType) {
    val result = cancerRepository.getCancerByName(cancerType);
    if (result.isEmpty()) {
      throw Status.INVALID_ARGUMENT.augmentDescription("Invalid cancer type '" + cancerType+"'").asRuntimeException();
    }
    return result.get();
  }

  private PrimarySiteEntity getPrimarySite(String primarySite) {
    val result = primarySiteRepository.getPrimarySiteByName(primarySite);
    if (result.isEmpty()) {
      throw Status.INVALID_ARGUMENT.augmentDescription("Invalid primary site" + primarySite).asRuntimeException();
    }
    return result.get();
  }

  private void processCancers(@NonNull ProgramEntity programToUpdate, @NonNull ProgramEntity updatingProgram)
          throws EmptyResultDataAccessException {
    if( !nullOrEmpty(updatingProgram.getProgramCancers())) {
      val updatingCancers = mapToImmutableSet(updatingProgram.getProgramCancers(), ProgramCancer::getCancer);
      val updatingCancerIds = convertToIds(updatingCancers);
      getManyEntities(CancerEntity.class, cancerRepository, updatingCancerIds);
      programToUpdate.setProgramCancers(updatingProgram.getProgramCancers());
    } else {
      programToUpdate.setProgramCancers(Collections.emptySet());
    }
  }

  private void processPrimarySites(@NonNull ProgramEntity programToUpdate, @NonNull ProgramEntity updatingProgram)
          throws EmptyResultDataAccessException {

    if( !nullOrEmpty(updatingProgram.getProgramPrimarySites())){
      val updatingPrimarySites = mapToImmutableSet(updatingProgram.getProgramPrimarySites(), ProgramPrimarySite::getPrimarySite);
      val updatingPrimarySiteIds = convertToIds(updatingPrimarySites);
      getManyEntities(PrimarySiteEntity.class, primarySiteRepository, updatingPrimarySiteIds);
      programToUpdate.setProgramPrimarySites(updatingProgram.getProgramPrimarySites());
     } else {
      programToUpdate.setProgramPrimarySites(Collections.emptySet());
    }
  }

  //TODO: add existence check, and fail with not found
  public void removeProgram(@NonNull ProgramEntity program) {
    egoService.cleanUpProgram(program);
    programRepository.deleteById(program.getId());
  }

  public void removeProgram(String name) throws EmptyResultDataAccessException {
    val p = getProgram(name);
    removeProgram(p);
  }

  public List<ProgramEntity> listPrograms() {
    return programRepository.findAll(new ProgramSpecificationBuilder()
      .setFetchCancers(true)
      .setFetchPrimarySites(true)
      .listAll());
  }

  public List<User> listUser(@NonNull UUID programId) {
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
