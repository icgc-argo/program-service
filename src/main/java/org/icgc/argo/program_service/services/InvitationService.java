package org.icgc.argo.program_service.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
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
import java.util.UUID;

import static java.lang.String.format;

@Service
@Slf4j
public class InvitationService {

  private final MailService mailService;
  private final JoinProgramInviteRepository invitationRepository;
  private final EgoService egoService;

  @Autowired
  InvitationService(
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
    val invitation = new JoinProgramInvite(program, userEmail, firstName, lastName, role);
    invitationRepository.save(invitation);
    mailService.sendInviteEmail(invitation);
    return invitation.getId();
  }

  @Transactional
  public List<JoinProgramInvite> listInvitations(@NonNull String programShortName) {
    return invitationRepository.findAllByProgramShortName(programShortName);
  }

  public JoinProgramInvite getInvitation(@NonNull UUID invitationId) throws NotFoundException {
    return invitationRepository
      .findById(invitationId)
      .orElseThrow(() ->
        new NotFoundException(format("Cannot find invitation with id '%s' ", invitationId)));
  }

  @Transactional
  public EgoUser acceptInvite(@NonNull JoinProgramInvite invitation) {
    invitation.accept();
    invitationRepository.save(invitation);
    egoService.joinProgram(invitation.getUserEmail(), invitation.getProgram().getShortName(), invitation.getRole());
    return egoService.convertInvitationToEgoUser(invitation);
  }

}
