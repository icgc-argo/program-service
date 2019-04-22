package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.JoinProgramInvitation;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.repositories.JoinProgramInvitationRepository;
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
  private final JoinProgramInvitationRepository invitationRepository;
  private final ProgramRepository programRepository;
  private final MailSender mailSender;

  public ProgramService(JoinProgramInvitationRepository invitationRepository, ProgramRepository programRepository, MailSender mailSender) {
    this.invitationRepository = invitationRepository;
    this.programRepository = programRepository;
    this.mailSender = mailSender;
  }

  public Optional<Program> getProgram(UUID uuid) {
    return programRepository.findById(uuid);
  }

  public void inviteUser(@NotNull Program program,
                         @Email @NotNull String userEmail,
                         @NotBlank @NotNull String firstName,
                         @NotBlank @NotNull String lastName,
                         @NotNull UserRole role) {
    val invitation = new JoinProgramInvitation(program, userEmail, firstName, lastName, role);
    invitationRepository.save(invitation);
    sendInvitation(invitation);
  }

  void sendInvitation(@NotNull JoinProgramInvitation invitation) {
    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setTo(invitation.getUserEmail());
//    TODO: Add invitation link
    msg.setText(
            "Dear " + invitation.getFirstName()
                    + invitation.getLastName()
                    + ", you are invited to join program.");
    mailSender.send(msg);
  }
}
