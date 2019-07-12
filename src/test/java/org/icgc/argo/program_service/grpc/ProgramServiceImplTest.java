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

package org.icgc.argo.program_service.grpc;

import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.icgc.argo.program_service.converter.ProgramConverterImpl;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ProgramService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramServiceImplTest {
  ProgramConverter programConverter = new ProgramConverterImpl(CommonConverter.INSTANCE);
  ProgramService programService = mock(ProgramService.class);
  InvitationService invitationService=mock(InvitationService.class);
  EgoService egoService = mock(EgoService.class);

  ProgramServiceImpl programServiceImpl = new ProgramServiceImpl(programService, programConverter,
    CommonConverter.INSTANCE,egoService, invitationService);

  @Test
  void createProgram() {
    val request = mock(CreateProgramRequest.class);
    val program = mock(Program.class);
    val responseObserver = mock(StreamObserver.class);

    when(request.getProgram()).thenReturn(program);
    when(programService.createProgram(program))
        .thenThrow(new DataIntegrityViolationException("test error"));

    programServiceImpl.createProgram(request, responseObserver);
    val argument = ArgumentCaptor.forClass(Exception.class);
    verify(responseObserver).onError(argument.capture());
    Assertions.assertThat(argument.getValue().getMessage()).as("Capture the error message").contains("test error");
  }

  @Test
  void removeProgram() {
    val request = mock(RemoveProgramRequest.class);
    when(request.getProgramShortName()).thenReturn(StringValue.of("TEST-XYZ123"));
    val responseObserver = mock(StreamObserver.class);

    doAnswer(invocation -> {
      throw new EmptyResultDataAccessException(1);
    }).when(programService).removeProgram(any(String.class));

    programServiceImpl.removeProgram(request, responseObserver);

    val argument = ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(responseObserver).onError(argument.capture());
    Assertions.assertThat(argument.getValue().getStatus().getCode()).as("Capture non exist exception").isEqualTo(Status.NOT_FOUND.getCode());
  }

  @Test
  void listUser() {
    val program = mock(ProgramEntity.class);
    val programName = "TEST-CA";
    val programId = UUID.randomUUID();
    program.setId(programId);

    val responseObserver = mock(StreamObserver.class);

    val accepted = LocalDateTime.now();
    val firstName = "Terry";
    val lastName = "Fox";
    val email ="tfox@national-heros.ca";
    val role = UserRole.COLLABORATOR;

    val invite1 = new JoinProgramInvite().
      setFirstName(firstName).
      setLastName(lastName).
      setProgram(program).
      setRole(role).
      setUserEmail(email).
      setStatus(JoinProgramInvite.Status.ACCEPTED).
      setAcceptedAt(accepted).
      setId(UUID.randomUUID()).
      setCreatedAt(accepted).
      setExpiresAt(accepted.plusMonths(3)).
      setEmailSent(true);

    val invitationList = List.of(invite1);

    when(program.getId()).thenReturn(programId);
    when(programService.getProgram(programName)).thenReturn(program);
    when(invitationService.listInvitations(programId)).thenReturn(invitationList);

    val request = ListUserRequest.newBuilder().
                  setProgramShortName(StringValue.of(programName)).build();

    val roleValue = UserRoleValue.newBuilder().setValue(role).build();

    val user1 = User.newBuilder().
      setEmail(StringValue.of(email)).
      setFirstName(StringValue.of(firstName)).
      setLastName(StringValue.of(lastName)).
      setRole(roleValue).
      build();

    val invitations = List.of(Invitation.newBuilder().
      setUser(user1).
      setAcceptedAt(CommonConverter.INSTANCE.localDateTimeToTimestamp(accepted))
      .setStatus(InviteStatus.ACCEPTED).
      build());

    val expected = ListUserResponse.newBuilder().addAllInvitations(invitations).build();
    programServiceImpl.listUser(request, responseObserver);
    verify(responseObserver).onNext(expected);

  }
}