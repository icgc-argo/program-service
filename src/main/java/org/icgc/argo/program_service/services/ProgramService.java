package org.icgc.argo.program_service.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.converter.FromProtoProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Validated
@Slf4j
public class ProgramService {

  /**
   * Dependencies
   */
  private final JoinProgramInviteRepository invitationRepository;
  private final ProgramRepository programRepository;
  private final FromProtoProgramConverter fromProtoProgramConverter;
  private final MailSender mailSender;
  private final EgoService egoService;

  @Autowired
  public ProgramService(@NonNull JoinProgramInviteRepository invitationRepository,
      @NonNull ProgramRepository programRepository,
      @NonNull FromProtoProgramConverter fromProtoProgramConverter,
      @NonNull MailSender mailSender,
      @NonNull EgoService egoService) {
    this.invitationRepository = invitationRepository;
    this.programRepository = programRepository;
    this.mailSender = mailSender;
    this.egoService = egoService;
    this.fromProtoProgramConverter = fromProtoProgramConverter;
  }

  //TODO: add existence check, and fail with not found
  public Optional<ProgramEntity> getProgram(@NonNull String name) {
    return programRepository.findByName(name);
  }

  //TODO: add existence check, and fail with not found
  public Optional<ProgramEntity> getProgram(@NonNull UUID uuid) {
    return programRepository.findById(uuid);
  }

  //TODO: add existence check, and ensure program doesnt already exist. If it does, return a Conflict
  public ProgramEntity createProgram(@NonNull Program program) {
    val programEntity = fromProtoProgramConverter.programToProgramEntity(program);

    // Set the timestamps
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    programRepository.save(programEntity);
    egoService.setUpProgram(programEntity);
    return programEntity;
  }

  //TODO: add existence check, and fail with not found
  public void removeProgram(@NonNull ProgramEntity program) {
    egoService.cleanUpProgram(program);
    programRepository.deleteById(program.getId());
  }

  public List<ProgramEntity> listPrograms() {
    return programRepository.findAll();
  }

  public UUID inviteUser(@NotNull ProgramEntity program,
                         @Email @NotNull String userEmail,
                         @NotBlank @NotNull String firstName,
                         @NotBlank @NotNull String lastName,
                         @NotNull UserRole role) {
    val invitation = new JoinProgramInvite(program, userEmail, firstName, lastName, role);
    sendInvite(invitation);
    invitationRepository.save(invitation);
    return invitation.getId();
  }

  void acceptInvite(JoinProgramInvite invitation) {
    invitation.accept();
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

    try {
      mailSender.send(msg);
      invitation.setEmailSent(true);
    } catch (MailAuthenticationException e) {
      log.info("Cannot log in to mail server", e);
    }
  }

  public Boolean acceptInvite(UUID invitationId) {
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

  public void removeProgram(UUID programId) {
    val program = programRepository.findById(programId);
    if (program.isPresent()) {
      removeProgram(program.get());
    } else {
      //TODO: add proper error handling
      log.error("Could not find program {}", programId);
    }
  }

}
