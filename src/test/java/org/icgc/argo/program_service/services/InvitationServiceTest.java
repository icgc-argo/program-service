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

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitationServiceTest {

  @Test
  void inviteUser() {
    val programEntity = mock(ProgramEntity.class);
    when(programEntity.getShortName()).thenReturn("TEST-CA");

    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);
    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val email = "user@example.com";

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
    assertEquals("First", ReflectionTestUtils.getField(invitation, "firstName"));
    assertEquals("Last", ReflectionTestUtils.getField(invitation, "lastName"));
    assertEquals(UserRole.ADMIN, ReflectionTestUtils.getField(invitation, "role"));
    verify(mailService).sendInviteEmail(ArgumentMatchers.any());

    val previousInvites1 = List.of(createInvite(programEntity, email, JoinProgramInviteEntity.Status.ACCEPTED),
      createInvite(programEntity, email, JoinProgramInviteEntity.Status.EXPIRED));

    // test for previously accepted invitations
    StatusRuntimeException exception=null;
    try {
      testInviteUserWithPreviouslyAcceptedInvitations(previousInvites1);
    } catch(StatusRuntimeException e) {
      exception=e;
    }
    assertNotNull(exception);
    assertTrue(exception.getStatus().getCode() == Status.Code.ALREADY_EXISTS);

    // test that previous pending invitations get set to INVALID.
    val previousInvites2= List.of(createInvite(programEntity, email, JoinProgramInviteEntity.Status.EXPIRED),
      createInvite(programEntity, email, JoinProgramInviteEntity.Status.PENDING));
    val saved_invitations = testInviteUserWithPreviouslyAcceptedInvitations(previousInvites2);
    assertNotNull(saved_invitations);
    assertEquals(2, saved_invitations.size());
    assert(saved_invitations.get(0).getStatus() == JoinProgramInviteEntity.Status.INVALID);
    assert(saved_invitations.get(1).getStatus() == JoinProgramInviteEntity.Status.PENDING);
  }

  JoinProgramInviteEntity createInvite(ProgramEntity programEntity, String email, JoinProgramInviteEntity.Status status) {
    return new JoinProgramInviteEntity().setProgram(programEntity).setUserEmail(email).setId(UUID.randomUUID()).
      setStatus(status).setCreatedAt(LocalDateTime.now()).setExpiresAt(LocalDateTime.now().plusDays(3));
  }

  @Test
  List<JoinProgramInviteEntity> testInviteUserWithPreviouslyAcceptedInvitations(List<JoinProgramInviteEntity> invites) {
    val programEntity = mock(ProgramEntity.class);
    when(programEntity.getShortName()).thenReturn("TEST-CA");

    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);
    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val programName="TEST-CA";
    val email="user@example.com";

    when(invitationRepository.findAllByProgramShortNameAndUserEmail(programName, email)).
      thenReturn(invites);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    invitationService.inviteUser(programEntity, "user@example.com", "First", "Last", UserRole.ADMIN);

    val invitationCaptor = ArgumentCaptor.forClass(JoinProgramInviteEntity.class);
    verify(invitationRepository, atMost(invites.size()+1)).save(invitationCaptor.capture());
    val invitations = invitationCaptor.getAllValues();

    return invitations;

  }

  @Test
  void previousPendingInvitations() {
    val programEntity = mock(ProgramEntity.class);
    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);
    val invitationRepository = mock(JoinProgramInviteRepository.class);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    invitationService.inviteUser(programEntity, "user@example.com", "First", "Last", UserRole.ADMIN);

  }

  @Test
  void acceptInvitation() {
    // ensure that accepting an invitation works
    testAcceptInvitation(JoinProgramInviteEntity.Status.PENDING);

    // ensure that we throw a Status exception if we try to join an invitation in a non-pending state.
    for(val status :JoinProgramInviteEntity.Status.values()) {
      if (status == JoinProgramInviteEntity.Status.PENDING) { continue; }
      Exception exception=null;
      try {
        testAcceptInvitation(status);
      } catch(StatusRuntimeException e) {
        exception = e;
      }
      assertNotNull(exception);
    }

  }

  void testAcceptInvitation(JoinProgramInviteEntity.Status status) {
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
    when(invitation.getStatus()).thenReturn(status);
    when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
    invitationService.acceptInvite(invitation.getId());
    verify(egoService)
      .joinProgram(invitation.getUserEmail(), invitation.getProgram().getShortName(), invitation.getRole());
    verify(invitation).accept();
  }

  @Test
  void getLatestInvitation() {
    String program="TEST-CA";
    val programEntity = mock(ProgramEntity.class);
    when(programEntity.getShortName()).thenReturn(program);
    val email = "user@example.com";

    val invite1=createInvite(programEntity, email, JoinProgramInviteEntity.Status.PENDING);
    val invite2=createInvite(programEntity, email, JoinProgramInviteEntity.Status.PENDING);
    val invite3=createInvite(programEntity, email, JoinProgramInviteEntity.Status.PENDING);

    // Take the first valid one in the list (invite3)
    val latest = testGetLatestInvitation(program, email, List.of(invite3, invite2, invite1));
    assertTrue(latest.isPresent());
    assertEquals(invite3.getId(), latest.get().getId());
    assertEquals(JoinProgramInviteEntity.Status.PENDING, latest.get().getStatus());

    // Ignore revoked and invalid entries. Ensure we recognize expired entries as expired.
    invite3.setStatus(JoinProgramInviteEntity.Status.REVOKED);
    invite2.setStatus(JoinProgramInviteEntity.Status.INVALID);
    invite1.setExpiresAt(invite1.getCreatedAt()); // make invitation1 expired.

    val latest2 = testGetLatestInvitation(program, email, List.of(invite3, invite2, invite1));
    assertTrue(latest2.isPresent());
    assertEquals(invite1.getId(), latest2.get().getId());
    assertEquals(JoinProgramInviteEntity.Status.EXPIRED, latest2.get().getStatus());

    // Now, invite2 is the first valid one
    invite2.setStatus(JoinProgramInviteEntity.Status.EXPIRED);
    val latest3 = testGetLatestInvitation(program, email, List.of(invite3, invite2, invite1));
    assertTrue(latest3.isPresent());
    assertEquals(invite2.getId(), latest3.get().getId());
    assertEquals(JoinProgramInviteEntity.Status.EXPIRED, latest3.get().getStatus());

    // all invalid => no invitation present
    invite2.setStatus(JoinProgramInviteEntity.Status.REVOKED);
    invite1.setStatus(JoinProgramInviteEntity.Status.INVALID);
    invite3.setStatus(JoinProgramInviteEntity.Status.INVALID);
    val latest4 = testGetLatestInvitation(program, email, List.of(invite3, invite2, invite1));
    assertTrue(latest4.isEmpty());

  }

  @Test
  void revokeInvitation() {
    String program="TEST-CA";
    val programEntity = mock(ProgramEntity.class);
    when(programEntity.getShortName()).thenReturn(program);

    val email = "user@example.com";

    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val invitations = List.of(createInvite(programEntity, email, JoinProgramInviteEntity.Status.PENDING),
      createInvite(programEntity, email, JoinProgramInviteEntity.Status.ACCEPTED));

    when(invitationRepository.findAllByProgramShortNameAndUserEmail(program, email)).thenReturn(invitations);
    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    invitationService.revoke(program, email);

  }

  Optional<JoinProgramInviteEntity> testGetLatestInvitation(String program, String email, List<JoinProgramInviteEntity> invites) {
    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    when(invitationRepository.findAllByProgramShortNameAndUserEmailOrderByCreatedAtDesc(program, email)).
      thenReturn(invites);
    val latestInvite = invitationService.getLatestInvitation(program, email);
    return latestInvite;
  }
}
