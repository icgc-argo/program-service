package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.mappers.ProgramMapper;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {
  private ProgramService programService;

  @Mock
  private Program program;

  @Mock
  private ProgramEntity programEntity;

  @Mock
  private JoinProgramInviteRepository invitationRepository;

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private MailSender mailSender;

  @Mock
  private JoinProgramInvite invitation;

  @Mock
  private ProgramMapper programMapper;

  @Mock
  private EgoService egoService;

  @BeforeEach
  void init() {
    this.programService = new ProgramService(invitationRepository, programRepository, mailSender, programMapper, egoService);
  }

  @Test
  void inviteUser() {
    programService.inviteUser(programEntity, "user@example.com", "First", "Last", UserRole.ADMIN);
    val invitationCaptor = ArgumentCaptor.forClass(JoinProgramInvite.class);
    verify(invitationRepository).save(invitationCaptor.capture());

    val invitation = invitationCaptor.getValue();
    assertThat(ReflectionTestUtils.getField(invitation, "program")).isEqualTo(programEntity);
    assertThat(ReflectionTestUtils.getField(invitation, "userEmail")).isEqualTo("user@example.com");
    assertThat((LocalDateTime) ReflectionTestUtils.getField(invitation, "createdAt"))
            .as("Creation time is within 5 seconds")
            .isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(5, ChronoUnit.SECONDS));
    assertThat((LocalDateTime) ReflectionTestUtils.getField(invitation, "expiredAt"))
            .as("Expire time should be after 47 hours")
            .isAfter(LocalDateTime.now(ZoneOffset.UTC).plusHours(47));
    assertThat(ReflectionTestUtils.getField(invitation, "status"))
            .as("Status is appending").isEqualTo(JoinProgramInvite.Status.PENDING);
    assertThat(ReflectionTestUtils.getField(invitation, "firstName"))
            .as("First name is first").isEqualTo("First");
    assertThat(ReflectionTestUtils.getField(invitation, "lastName"))
            .as("Last name is first").isEqualTo("Last");
    assertThat(ReflectionTestUtils.getField(invitation, "role"))
            .as("Role is admin").isEqualTo(UserRole.ADMIN);

    val messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(messageCaptor.capture());
    val message = messageCaptor.getValue();
    assertThat(message.getTo()).contains("user@example.com");
    assertThat(invitation.getEmailSent())
            .as("emailSent should have been set to true")
            .isTrue();
  }

  @Test
  void acceptInvitation() {
    programService.acceptInvite(invitation);
    verify(invitation).accept();
  }

  @Test
  void createProgram() {
    when(programMapper.ProgramToProgramEntity(program)).thenReturn(programEntity);
    programService.createProgram(program);
    verify(programRepository).save(programEntity);
  }

  @Test
  void listPrograms() {
    when(programRepository.findAll()).thenReturn(List.of(programEntity));
    when(programMapper.ProgramEntityToProgram(programEntity)).thenReturn(program);
    val programs = programService.listPrograms();
    assertThat(programs).contains(program);
  }
}