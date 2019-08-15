package org.icgc.argo.program_service.services;

import io.grpc.Status;
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
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity.Status.*;

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
    val programShortName = program.getShortName();
    val invitation = new JoinProgramInviteEntity(program, userEmail, firstName, lastName, role);
    val previousInvitations = invitationRepository.findAllByProgramShortNameAndUserEmail(programShortName, userEmail);

    // 1) If there is an accepted invitation, then return an error saying the user has accepted the invitation
    if (previousInvitations.stream().anyMatch(i -> i.getStatus() == ACCEPTED)) {
      throw Status.ALREADY_EXISTS.augmentDescription("Invitation was already accepted").asRuntimeException();
    }
    // 2) If there is a PENDING invitation, then that previous invitation should be set to INVALID.
    //If a previous invitation is EXPIRED,REVOKED or INVALID, leave them as is.
    previousInvitations.stream().filter(i -> i.getStatus() == PENDING).map(i -> i.setStatus(INVALID)).
      forEach(i -> invitationRepository.save(i));

    invitationRepository.save(invitation);
    mailService.sendInviteEmail(invitation);
    return invitation.getId();
  }

  @Transactional
  public EgoUser acceptInvite(@NonNull UUID invitationId) throws NotFoundException {
    val invitation = invitationRepository
      .findById(invitationId)
      .orElseThrow(() ->
        new NotFoundException(format("Cannot find invitation with id '%s' ", invitationId)));

    if (invitation.getStatus() != PENDING) {
      if (invitation.getStatus() == null) {
        throw Status.FAILED_PRECONDITION.augmentDescription(
          "Cannot accept invitation because it is in state(NULL), not PENDING").asRuntimeException();
      }
      throw Status.FAILED_PRECONDITION.augmentDescription(
        format("Cannot accept invitation because it is in state(%s), not PENDING", invitation.getStatus().toString())).
        asRuntimeException();
    }

    invitation.accept();
    invitationRepository.save(invitation);
    egoService.joinProgram(invitation.getUserEmail(), invitation.getProgram().getShortName(), invitation.getRole());
    return egoService.convertInvitationToEgoUser(invitation);
  }

  public Optional<JoinProgramInviteEntity> getLatestInvitation(String programShortName, String email) {
    val invitations = listInvitations(programShortName, email);
    val validInvitations = invitations.stream().filter(i -> i.getStatus() != INVALID && i.getStatus() != REVOKED).
      collect(Collectors.toList());
    if (validInvitations.size() == 0) {
      return Optional.empty();
    }
    return Optional.of(validInvitations.get(0));
  }

  @Transactional
  public void revoke(String programShortName, String email) {
    val previousInvitations = invitationRepository.findAllByProgramShortNameAndUserEmail(programShortName, email);
    previousInvitations.stream().filter(i -> i.getStatus() == PENDING || i.getStatus() == ACCEPTED).
      map(i -> i.setStatus(REVOKED)).forEach(i -> invitationRepository.save(i));
  }


  public Optional<JoinProgramInviteEntity> getInvitationById(UUID invitationId) {
    return invitationRepository.findById(invitationId);
  }

  public List<JoinProgramInviteEntity> listPendingInvitations(String programShortName) {
    return invitationRepository.findAllByProgramShortNameAndStatus(programShortName, PENDING);
  }

  public List<JoinProgramInviteEntity> listInvitations(String programShortName, String email) {
    return invitationRepository.findAllByProgramShortNameAndUserEmailOrderByCreatedAtDesc(programShortName, email);
  }
}