package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;
@Service
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

  public boolean acceptInvite(UUID invitationId) {
    val invitation = invitationRepository.findById(invitationId);
    if (invitation.isPresent()) {
      val i = invitation.get();
      i.accept();
      val email = invitation.get().getUserEmail();
      egoService.joinProgram(email, i.getProgram().getShortName(), i.getRole());
      invitationRepository.save(invitation.get());
      return true;
    } else {
      return false;
    }
  }
}
