/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.icgc.argo.program_service.services;

import org.icgc.argo.program_service.converter.CommonConverter;
import lombok.val;
import net.bytebuddy.utility.RandomString;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.CancerRepository;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.PrimarySiteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mail.MailSender;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

  @InjectMocks
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
  private CancerRepository cancerRepository;

  @Mock
  private MailSender mailSender;

  @Mock
  private JoinProgramInvite invitation;

  @Mock
  private ProgramConverter programConverter;

  @Mock
  private EgoService egoService;

  @Mock
  private MailService mailService;

  @Mock
  private PrimarySiteRepository primarySiteRepository;

  @Mock
  private CommonConverter commonConverter;

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

    verify(mailService).sendInviteEmail(ArgumentMatchers.any());
  }

  @Test
  void acceptInvitation() {
    when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
    programService.acceptInvite(invitation.getId());
    verify(egoService).joinProgram(invitation.getUserEmail(), invitation.getProgram(), invitation.getRole());
    verify(invitation).accept();
  }

  @Test
  void createProgram() {
    val inputProgramEntity = new ProgramEntity().setName(RandomString.make(10)).setShortName(RandomString.make(33));
    assertThat(inputProgramEntity.getCreatedAt()).isNull();
    assertThat(inputProgramEntity.getUpdatedAt()).isNull();
    when(programConverter.programToProgramEntity(program)).thenReturn(inputProgramEntity);
    val outputEntity = programService.createProgram(program, List.of());
    assertThat(outputEntity.getCreatedAt()).isNotNull();
    assertThat(outputEntity.getUpdatedAt()).isNotNull();
    verify(programRepository).save(inputProgramEntity);
  }

  @Test
  void listPrograms() {
    when(programRepository.findAll((Specification<ProgramEntity>)Mockito.any()))
        .thenReturn(List.of(programEntity));
    val programs = programService.listPrograms();
    assertThat(programs).contains(programEntity);
  }

}
