package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;
import java.util.UUID;

@Service
@Validated
public class ProgramService {
  private final JoinProgramInviteRepository invitationRepository;
  private final ProgramRepository programRepository;
  private final MailSender mailSender;

  public ProgramService(JoinProgramInviteRepository invitationRepository, ProgramRepository programRepository, MailSender mailSender) {
    this.invitationRepository = invitationRepository;
    this.programRepository = programRepository;
    this.mailSender = mailSender;
  }

  public Optional<ProgramEntity> getProgram(UUID uuid) {
    return programRepository.findById(uuid);
  }

  public void inviteUser(@NotNull ProgramEntity program,
                         @Email @NotNull String userEmail,
                         @NotBlank @NotNull String firstName,
                         @NotBlank @NotNull String lastName,
                         @NotNull UserRole role) {
    val invitation = new JoinProgramInvite(program, userEmail, firstName, lastName, role);
    sendInvite(invitation);
    invitationRepository.save(invitation);
  }

  private void sendInvite(@NotNull JoinProgramInvite invitation) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(invitation.getUserEmail());
//    TODO: Add invitation link
    msg.setText(
            "Dear " + invitation.getFirstName()
                    + invitation.getLastName()
                    + ", you are invited to join program.");

    mailSender.send(msg);
    invitation.setEmailSent(true);
  }

  public void acceptInvite(JoinProgramInvite invitation) {
    invitation.accept();
    invitationRepository.save(invitation);
  }
}
