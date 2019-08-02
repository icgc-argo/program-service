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

import lombok.val;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class InvitationServiceTest {

  @Test
  void inviteUser() {
    val programEntity = mock(ProgramEntity.class);
    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);
    val invitationRepository = mock(JoinProgramInviteRepository.class);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    invitationService.inviteUser(programEntity, "user@example.com", "First", "Last", UserRole.ADMIN);
    val invitationCaptor = ArgumentCaptor.forClass(JoinProgramInviteEntity.class);
    verify(invitationRepository).save(invitationCaptor.capture());

    val invitation = invitationCaptor.getValue();
    assertEquals(ReflectionTestUtils.getField(invitation, "program"), programEntity);
    assertEquals(ReflectionTestUtils.getField(invitation, "userEmail"), "user@example.com");
    assertTrue(((LocalDateTime) (ReflectionTestUtils.getField(invitation, "createdAt"))).
      isAfter(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(5)));
    assertTrue(((LocalDateTime) ReflectionTestUtils.getField(invitation, "expiresAt"))
      .isAfter(LocalDateTime.now(ZoneOffset.UTC).plusHours(47)));
    assertEquals(JoinProgramInviteEntity.Status.PENDING, ReflectionTestUtils.getField(invitation, "status"));
    assertEquals( "First", ReflectionTestUtils.getField(invitation, "firstName"));
    assertEquals( "Last", ReflectionTestUtils.getField(invitation, "lastName"));
    assertEquals( UserRole.ADMIN, ReflectionTestUtils.getField(invitation, "role"));

    verify(mailService).sendInviteEmail(ArgumentMatchers.any());
  }

  @Test
  void acceptInvitation() {
    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    val invitation = mock(JoinProgramInviteEntity.class);
    val program = new ProgramEntity();
    program.setShortName("TEST1");
    when(invitation.getProgram()).thenReturn(program);
    when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
    when(invitation.getId()).thenReturn(UUID.randomUUID());
    when(invitation.getStatus()).thenReturn(JoinProgramInviteEntity.Status.PENDING);
    when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
    invitationService.acceptInvite(invitation.getId());
    verify(egoService).joinProgram(invitation.getUserEmail(), invitation.getProgram().getShortName(), invitation.getRole());
    verify(invitation).accept();
  }

  @Test
  void listInvitations() {
    val programShortName="TEST-CA";
    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    val invitation1 = new JoinProgramInviteEntity();
    when(invitationRepository.findAllByProgramShortName(programShortName)).thenReturn(List.of(invitation1));
    val result = invitationService.listInvitations(programShortName);
    assertEquals(result, List.of(invitation1));
  }

}
