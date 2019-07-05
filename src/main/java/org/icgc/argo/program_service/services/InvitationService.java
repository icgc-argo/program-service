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
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
    MailService mailService,
    JoinProgramInviteRepository invitationRepository,
    EgoService egoService
  ) {
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

  public EgoUser acceptInvite(UUID invitationId) throws NotFoundException {
    val invitation = invitationRepository
            .findById(invitationId)
            .orElseThrow(() ->
              new NotFoundException(format("Cannot find invitation with id '%s' ", invitationId)));
    invitation.accept();
    egoService.joinProgram(invitation.getUserEmail(), invitation.getProgram().getShortName(), invitation.getRole());
    invitationRepository.save(invitation);
    return convertInvitationToEgoUser(invitation);
  }

  private EgoUser convertInvitationToEgoUser(@NonNull JoinProgramInvite invite){
    return EgoUser.builder()
            .email(invite.getUserEmail())
            .firstName(invite.getFirstName())
            .lastName(invite.getLastName())
            .role(invite.getRole())
            .build();
  }
}
