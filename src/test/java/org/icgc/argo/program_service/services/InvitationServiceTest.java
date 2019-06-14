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
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoGroup;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val egoService = mock(EgoService.class);
    val mailService = mock(MailService.class);

    val invitationService = new InvitationService(mailService, invitationRepository, egoService);
    val invitation = mock(JoinProgramInvite.class);
    when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
    invitationService.acceptInvite(invitation.getId());
    verify(egoService).joinProgram(invitation.getUserEmail(), invitation.getProgram(), invitation.getRole());
    verify(invitation).accept();
  }

}
