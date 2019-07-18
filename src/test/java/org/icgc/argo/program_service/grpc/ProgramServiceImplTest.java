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
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.persistence.criteria.Join;
import java.time.LocalDateTime;
import java.util.*;

import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
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
    // Case 1: No pending invitations, user in ego with invitation information
    val responseObserver = mock(StreamObserver.class);

    val programName1 = "TEST-CA";
    val program1 = mockProgram(programName1);
    val invite1 = createInvitation(program1);

    when(invitationService.listPendingInvitations(programName1)).thenReturn(List.of());
    when(invitationService.getInvitation(programName1, invite1.getUserEmail())).thenReturn(Optional.of(invite1));

    val roleValue = UserRoleValue.newBuilder().setValue(invite1.getRole()).build();

    val user1 = User.newBuilder().
      setEmail(StringValue.of(invite1.getUserEmail())).
      setFirstName(StringValue.of(invite1.getFirstName())).
      setLastName(StringValue.of(invite1.getLastName())).
      setRole(roleValue).
      build();

    when(egoService.getUsersInProgram(programName1)).thenReturn(List.of(user1));

    val invitations = List.of(invite1);
    val request = createListUserRequest(programName1);

    val expected = programConverter.invitationsToListUserResponse(invitations);

    programServiceImpl.listUser(request, responseObserver);
    verify(responseObserver).onNext(expected);
  }

  @Test
  void listUser2() {
    val programName = "TEST-CA";
    val program = mockProgram(programName);

    val pendingInvitations = List.of(createPendingInvitation(program),
      createPendingInvitation(program));

    val invite1 = createInvitation(program);
    val invite2 = createInvitation(program);
    val user1 = fromInvite(invite1);
    val user2 = fromInvite(invite2);
    val egoUsers = List.of(user1, user2);

    val egoInvitations = new TreeMap<String, JoinProgramInvite>();
    egoInvitations.put(user1.getEmail().getValue(), invite1);

    val service = setupListUserTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUserRequest(programName);
    val invitations = new ArrayList<JoinProgramInvite>(pendingInvitations);
    invitations.add(invite1);
    invite2.setStatus(null);
    invite2.setAcceptedAt(null);
    invitations.add(invite2);

    val expected = programConverter.invitationsToListUserResponse(invitations);
    val responseObserver = mock(StreamObserver.class);

    service.listUser(request, responseObserver);
    val argument = ArgumentCaptor.forClass(ListUserResponse.class);

    verify(responseObserver).onNext(argument.capture());
    val actualInvitations = Set.copyOf(argument.getValue().getInvitationsList());
    val expectedInvitations = Set.copyOf(expected.getInvitationsList());

    assertTrue(actualInvitations.containsAll(expectedInvitations));
    assertTrue(expectedInvitations.containsAll(actualInvitations));

  }

  User fromInvite(JoinProgramInvite invite) {
    val roleValue = UserRoleValue.newBuilder().setValue(invite.getRole()).build();

   return User.newBuilder().
      setEmail(StringValue.of(invite.getUserEmail())).
      setFirstName(StringValue.of(invite.getFirstName())).
      setLastName(StringValue.of(invite.getLastName())).
      setRole(roleValue).
      build();
  }

  @Test
  void listUser3() {
    val programName1 = "TEST-CA";
    val program1 = mockProgram(programName1);
    val invite1 = createInvitation(program1);

    val roleValue = UserRoleValue.newBuilder().setValue(invite1.getRole()).build();

    val user1 = User.newBuilder().
      setEmail(StringValue.of(invite1.getUserEmail())).
      setFirstName(StringValue.of(invite1.getFirstName())).
      setLastName(StringValue.of(invite1.getLastName())).
      setRole(roleValue).
      build();

    val pendingInvitations = new ArrayList<JoinProgramInvite>();
    val egoUsers = List.of(user1);

    val egoInvitations = new TreeMap<String, JoinProgramInvite>();
    egoInvitations.put(user1.getEmail().getValue(), invite1);

    val service = setupListUserTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUserRequest(programName1);
    val expected = programConverter.invitationsToListUserResponse(List.of(invite1));
    val responseObserver = mock(StreamObserver.class);

    service.listUser(request, responseObserver);
    verify(responseObserver).onNext(expected);
  }

  ProgramServiceImpl setupListUserTest(
      String programName,
      List <JoinProgramInvite> pendingInvitations,
      List<User> egoUsers,
      Map<String, JoinProgramInvite> egoInvitations) {
    ProgramConverter programConverter = new ProgramConverterImpl(CommonConverter.INSTANCE);
    ProgramService programService = mock(ProgramService.class);
    InvitationService invitationService=mock(InvitationService.class);
    EgoService egoService = mock(EgoService.class);

    for(val key: egoInvitations.keySet()) {
      when(invitationService.getInvitation(programName, key)).thenReturn(Optional.of(egoInvitations.get(key)));
    }

    when(invitationService.listPendingInvitations(programName)).thenReturn(pendingInvitations);
    when(egoService.getUsersInProgram(programName)).thenReturn(egoUsers);

    return new ProgramServiceImpl(programService, programConverter, CommonConverter.INSTANCE,egoService,
      invitationService);
  }

  ListUserRequest createListUserRequest(String shortName) {
    return ListUserRequest.newBuilder().
      setProgramShortName(StringValue.of(shortName)).build();
  }

  ListUserResponse getListUserResponse(List<Invitation> invitations) {
    return ListUserResponse.newBuilder().addAllInvitations(invitations).build();
  }

  Invitation createInvitation(User user, LocalDateTime accepted, InviteStatus status) {
    val builder =  Invitation.newBuilder().
      setUser(user).setStatus(InviteStatusValue.newBuilder().setValue(status).build());
    if (accepted == null) {
      return builder.build();
    }
    return builder. setAcceptedAt(CommonConverter.INSTANCE.localDateTimeToTimestamp(accepted)).build();
  }

  ProgramEntity mockNonExistantProgram(String shortName) {
    val program = mock(ProgramEntity.class);
    when(programService.getProgram(shortName)).thenThrow(programNotFound(shortName));
    return program;
  }

  StatusRuntimeException programNotFound(String shortName) {
    return new StatusRuntimeException(
      Status.fromCode(Status.Code.NOT_FOUND).withDescription("Program '"+shortName+"' not found"));
  }

  ProgramEntity mockProgram(String shortName) {
    val program = mock(ProgramEntity.class);
    when(programService.getProgram(shortName)).thenReturn(program);

    return program;
  }

  JoinProgramInvite createInvitationWithStatus(ProgramEntity program,
    JoinProgramInvite.Status status) {
      val created = LocalDateTime.now();

      val firstName = RandomStringUtils.randomAlphabetic(5);
      val lastName = RandomStringUtils.randomAlphabetic(10);

      val invite = new JoinProgramInvite().
        setFirstName(firstName).
        setLastName(lastName).
        setProgram(program).
        setRole(RandomChoiceFrom(UserRole.values())).
        setUserEmail(firstName + "." + lastName + "@gmail.com").
        setStatus(status).
        setId(UUID.randomUUID()).
        setCreatedAt(created).
        setExpiresAt(created.plusMonths(3)).
        setEmailSent(true);
      if (status == JoinProgramInvite.Status.PENDING) {
        return invite;
      }
    val accepted = created.plusDays(nextInt(90));
      return invite.setAcceptedAt(accepted);
  }

  JoinProgramInvite createPendingInvitation(ProgramEntity program) {
    return createInvitationWithStatus(program, JoinProgramInvite.Status.PENDING);
  }

  JoinProgramInvite createInvitation(ProgramEntity program) {
    return createInvitationWithStatus(program,
      RandomChoiceFrom(JoinProgramInvite.Status.values()));
  }

  <T> T RandomChoiceFrom(T[] elements) {
    return elements[nextInt(elements.length-1)];
  }

}