package org.icgc.argo.program_service.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;

@Service
@Slf4j
public class InvitationService {

  private final MailService mailService;
  private final JoinProgramInviteRepository invitationRepository;
  private final EgoService egoService;

  @Autowired InvitationService(
    @NonNull MailService mailService,
    @NonNull JoinProgramInviteRepository invitationRepository,
    @NonNull EgoService egoService) {
    this.mailService = mailService;
    this.invitationRepository = invitationRepository;
    this.egoService = egoService;
  }

  public UUID inviteUser(@NotNull ProgramEntity program,
    @Email @NotNull String userEmail,
    @NotBlank @NotNull String firstName,
    @NotBlank @NotNull String lastName,
    @NotNull UserRole role) {
    val invitation = new JoinProgramInviteEntity(program, userEmail, firstName, lastName, role);
    invitationRepository.save(invitation);
    mailService.sendInviteEmail(invitation);
    return invitation.getId();
  }

  @Transactional
  public List<JoinProgramInviteEntity> listInvitations(@NonNull String programShortName) {
    return invitationRepository.findAllByProgramShortName(programShortName);
  }

  @Transactional
  public EgoUser acceptInvite(@NonNull UUID invitationId) throws NotFoundException {
    val invitation = invitationRepository
      .findById(invitationId)
      .orElseThrow(() ->
        new NotFoundException(format("Cannot find invitation with id '%s' ", invitationId)));

    invitation.accept();
    invitationRepository.save(invitation);
    egoService.joinProgram(invitation.getUserEmail(), invitation.getProgram().getShortName(), invitation.getRole());
    return egoService.convertInvitationToEgoUser(invitation);
  }

  public Optional<JoinProgramInviteEntity> getInvitation(String programShortName, String email) {
    return invitationRepository.findTopByProgramShortNameAndUserEmailOrderByCreatedAtDesc(programShortName, email);
  }

  public Optional<JoinProgramInviteEntity> getInvitation(UUID invitationId) {
    return invitationRepository.findById(invitationId);
  }

  public List<JoinProgramInviteEntity> listPendingInvitations(String programShortName) {
    return invitationRepository.findAllByProgramShortNameAndStatus(programShortName, JoinProgramInviteEntity.Status.PENDING);
  }
}