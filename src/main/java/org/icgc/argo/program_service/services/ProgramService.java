package org.icgc.argo.program_service.services;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.mappers.ProgramMapper;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
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
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
public class ProgramService {
  private final JoinProgramInviteRepository invitationRepository;
  private final ProgramRepository programRepository;
  private final MailSender mailSender;
  private final ProgramMapper programMapper;
  private final EgoService egoService;


  public ProgramService(JoinProgramInviteRepository invitationRepository, ProgramRepository programRepository, MailSender mailSender, ProgramMapper programMapper, EgoService egoService) {
    this.invitationRepository = invitationRepository;
    this.programRepository = programRepository;
    this.mailSender = mailSender;
    this.programMapper = programMapper;
    this.egoService = egoService;
  }

  public Optional<ProgramEntity> getProgram(String name) {
    return programRepository.findByName(name);
  }

  public Optional<ProgramEntity> getProgram(UUID uuid) {
    return programRepository.findById(uuid);
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

  public ProgramEntity createProgram(Program program) {
    val programEntity = programMapper.ProgramToProgramEntity(program);
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    val entity = programRepository.save(programEntity);
    egoService.setUpProgram(programEntity);
    return entity;
  }

  public void removeProgram(ProgramEntity program) {
    egoService.cleanUpProgram(program);
    programRepository.deleteById(program.getId());
  }

  public List<Program> listPrograms() {
    val programEntities = programRepository.findAll();

    return programEntities.stream()
            .map(programMapper::ProgramEntityToProgram)
            .collect(Collectors.toUnmodifiableList());
  }
}
