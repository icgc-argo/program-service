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

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.properties.ValidationProperties;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.AuthorizationService;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.validation.ValidatorFactory;
import javax.validation.constraints.Email;
import java.time.LocalDateTime;
import java.util.*;

import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceImplTest {
  ProgramConverter programConverter = ProgramConverter.INSTANCE;
  ProgramService programService = mock(ProgramService.class);
  InvitationService invitationService = mock(InvitationService.class);
  EgoService egoService = mock(EgoService.class);
  AuthorizationService authorizationService = mock(AuthorizationService.class);
  ValidationProperties properties = new ValidationProperties();
  ValidatorFactory validatorFactory = properties.factory();

  ProgramServiceImpl programServiceImpl = new ProgramServiceImpl(programService, programConverter,
    CommonConverter.INSTANCE, egoService, invitationService, authorizationService, validatorFactory);

  @Test
  void createProgramNoProgram() {
    val request = mock(CreateProgramRequest.class);
    val responseObserver = mock(StreamObserver.class);
    try {
      programServiceImpl.createProgram(request, responseObserver);
      fail("Didn't throw exception for missing program");
    } catch (Exception ex) {
      assertEquals("Incorrect error message", "INVALID_ARGUMENT: Program must be specified",
        ex.getMessage());
    }

  }

  @Test
  void createProgramNoAdmins() {
    val responseObserver = mock(StreamObserver.class);
    val program = Program.newBuilder().setName(StringValue.of("TEST1-CA"));
    val admins = new ArrayList<User>();
    val request = CreateProgramRequest.newBuilder().setProgram(program).
      addAllAdmins(admins).build();

    try {
      programServiceImpl.createProgram(request, responseObserver);
      fail("Didn't throw exception for missing administrators");
    } catch (Exception ex) {
      assertEquals("Incorrect error message",
        "INVALID_ARGUMENT: Program must have at least one administrator",
        ex.getMessage());
    }
  }

  @Test
  void createProgramInvalidEmail() {
    val responseObserver = mock(StreamObserver.class);
    val program = Program.newBuilder().setName(StringValue.of("TEST1-CA"));
    val admins = List.of(User.newBuilder().
      setEmail(StringValue.of("invalid")).
      setFirstName(StringValue.of("Wont")).
      setLastName(StringValue.of("Work")).
      setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN).build()).
      build());
    val request = CreateProgramRequest.newBuilder().setProgram(program).
      addAllAdmins(admins).build();

    try {
      programServiceImpl.createProgram(request, responseObserver);
      fail("Didn't throw exception for invalid administrator email address");
    } catch (Exception ex) {
      assertEquals("Incorrect error message",
        "INVALID_ARGUMENT: Invalid email address 'invalid' for admin 'Wont Work'",
        ex.getMessage());
    }
  }

  @Test
  void createProgramEmptyProgram() {
    val responseObserver = mock(StreamObserver.class);
    val program = Program.newBuilder().build();
    val admins = List.of(User.newBuilder().
      setEmail(StringValue.of("valid@com.org")).
      setFirstName(StringValue.of("Will ")).
      setLastName(StringValue.of("Work")).
      setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN).build()).
      build());
    val request = CreateProgramRequest.newBuilder().setProgram(program).
      addAllAdmins(admins).build();

    try {
      programServiceImpl.createProgram(request, responseObserver);
      fail("Didn't throw exception for invalid program short name");
    } catch (Exception ex) {
      assertEquals("Incorrect error message",
        "INVALID_ARGUMENT: Invalid program: commitmentDonors must not be null, "
          + "genomicDonors must not be null, membershipType must not be null, name must not be null, "
          + "shortName must not be null, submittedDonors must not be null, website must not be null",
        ex.getMessage());
    }
  }

  @Test
  void createProgramInvalidShortName() {
    val responseObserver = mock(StreamObserver.class);

    val admins = List.of(User.newBuilder().
      setEmail(StringValue.of("valid@com.org")).
      setFirstName(StringValue.of("Will ")).
      setLastName(StringValue.of("Work")).
      setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN).build()).
      build());

    val program = Program.newBuilder().
      setCommitmentDonors(Int32Value.of(1000)).
      setGenomicDonors(Int32Value.of(0)).
      setMembershipType(MembershipTypeValue.newBuilder().setValueValue(MembershipType.ASSOCIATE_VALUE).build()).
      setName(StringValue.of("A wonderful research program")).
      setShortName(StringValue.of("Invalid shortname")).
      setWebsite(StringValue.of("http://www.site.com")).
      setSubmittedDonors(Int32Value.of(0)).
      build();

    val request = CreateProgramRequest.newBuilder().setProgram(program).
      addAllAdmins(admins).build();

    try {
      programServiceImpl.createProgram(request, responseObserver);
      fail("Didn't throw exception for invalid program short name");
    } catch (Exception ex) {
      assertEquals("Incorrect error message",
        "INVALID_ARGUMENT: Invalid program: shortName is invalid",
        ex.getMessage());
    }
  }

  @Test
  void createProgramInvalidWebSite() {
    val responseObserver = mock(StreamObserver.class);

    val admins = List.of(User.newBuilder().
      setEmail(StringValue.of("valid@com.org")).
      setFirstName(StringValue.of("Will ")).
      setLastName(StringValue.of("Work")).
      setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN).build()).
      build());

    val program = Program.newBuilder().
      setCommitmentDonors(Int32Value.of(1000)).
      setGenomicDonors(Int32Value.of(0)).
      setMembershipType(MembershipTypeValue.newBuilder().setValueValue(MembershipType.ASSOCIATE_VALUE).build()).
      setName(StringValue.of("A wonderful research program")).
      setShortName(StringValue.of("TEST1-CA")).
      setWebsite(StringValue.of("site")).
      setSubmittedDonors(Int32Value.of(0)).
      build();

    val request = CreateProgramRequest.newBuilder().setProgram(program).
      addAllAdmins(admins).build();

    try {
      programServiceImpl.createProgram(request, responseObserver);
      fail("Didn't throw exception for invalid web site");
    } catch (Exception ex) {
      assertEquals("Incorrect error message",
        "INVALID_ARGUMENT: Invalid program: website must be a valid URL",
        ex.getMessage());
    }
  }

  // Test currently stil in development
//  @Test
//  void createProgramMissingCancerTypes() {
//    val responseObserver = mock(StreamObserver.class);
//
//    val admins = List.of(User.newBuilder().
//      setEmail(StringValue.of("valid@com.org")).
//      setFirstName(StringValue.of("Will ")).
//      setLastName(StringValue.of("Work")).
//      setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN).build()).
//      build());
//
//    val program = Program.newBuilder().
//      setCommitmentDonors(Int32Value.of(1000)).
//      setGenomicDonors(Int32Value.of(0)).
//      setMembershipType(MembershipTypeValue.newBuilder().setValueValue(MembershipType.ASSOCIATE_VALUE).build()).
//      setName(StringValue.of("A wonderful research program")).
//      setShortName(StringValue.of("TEST1-CA")).
//      setWebsite(StringValue.of("http://www.site.com")).
//      setSubmittedDonors(Int32Value.of(0)).
//      build();
//
//    val request = CreateProgramRequest.newBuilder().setProgram(program).
//      addAllAdmins(admins).build();
//
//    try {
//      programServiceImpl.createProgram(request, responseObserver);
//      fail("Didn't throw exception for missing cancer types");
//    } catch (Exception ex) {
//      assertEquals("Incorrect error message",
//        "INVALID_ARGUMENT: Invalid program: website must be a valid URL",
//        ex.getMessage());
//    }
//  }

  @Test
  void removeProgram() {
    val request = mock(RemoveProgramRequest.class);
    when(request.getProgramShortName()).thenReturn(StringValue.of("TEST-XYZ123"));
    val responseObserver = mock(StreamObserver.class);

    doAnswer(invocation -> {
      throw new EmptyResultDataAccessException(1);
    }).when(programService).removeProgram(any(String.class));

    try {
      programServiceImpl.removeProgram(request, responseObserver);
    } catch (Exception ex) {
      assertEquals("NOT_FOUND: Incorrect result size: expected 1, actual 0", ex.getMessage());

      assertTrue(ex instanceof StatusRuntimeException);
      val e = (StatusRuntimeException) ex;
      assertEquals(Status.NOT_FOUND.getCode(), e.getStatus().getCode());
    }
  }

  @Test
  void listUsers() {
    val responseObserver = mock(StreamObserver.class);

    val programName1 = "TEST-CA";
    val program1 = mockProgram(programName1);
    val invite1 = createInvitation(program1);

    when(invitationService.listPendingInvitations(programName1)).thenReturn(List.of());
    when(invitationService.getLatestInvitation(programName1, invite1.getUserEmail())).thenReturn(Optional.of(invite1));


    val roleValue = UserRoleValue.newBuilder().setValue(invite1.getRole()).build();

    val user1 = User.newBuilder().
      setEmail(StringValue.of(invite1.getUserEmail())).
      setFirstName(StringValue.of(invite1.getFirstName())).
      setLastName(StringValue.of(invite1.getLastName())).
      setRole(roleValue).
      build();

    when(egoService.getUsersInProgram(programName1)).thenReturn(List.of(user1));

    val invitations = List.of(invite1);
    val request = createListUsersRequest(programName1);

    val expected = programConverter.invitationsToListUsersResponse(invitations);

    programServiceImpl.listUsers(request, responseObserver);
    verify(responseObserver).onNext(expected);
  }

  @Test
  void listUsers1() {
    // Case 1: No pending invitations, 1 user in ego with matching
    // invitation information in the invitation table
    val programName1 = "TEST-CA";
    val program1 = mockProgram(programName1);
    val invite1 = createInvitation(program1);
    val user1 = fromInvite(invite1);

    val pendingInvitations = new ArrayList<JoinProgramInviteEntity>();
    val egoUsers = List.of(user1);

    val egoInvitations = new TreeMap<String, JoinProgramInviteEntity>();
    egoInvitations.put(user1.getEmail().getValue(), invite1);

    val service = setupListUsersTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUsersRequest(programName1);
    val expected = programConverter.invitationsToListUsersResponse(List.of(invite1));
    val responseObserver = mock(StreamObserver.class);

    service.listUsers(request, responseObserver);
    verify(responseObserver).onNext(expected);
  }

  @Test
  void listUsers2() {
    // Case 2: Two pending invitations, and two ego users -- one has
    // a matching invitation record in the invitation table, and the other doesn't.
    val programName = "TEST-CA";
    val program = mockProgram(programName);

    val pendingInvitations = List.of(createPendingInvitation(program),
      createPendingInvitation(program));

    val invite1 = createInvitation(program);
    val invite2 = createInvitation(program);
    val user1 = fromInvite(invite1);
    val user2 = fromInvite(invite2);
    val egoUsers = List.of(user1, user2);

    val egoInvitations = new TreeMap<String, JoinProgramInviteEntity>();
    egoInvitations.put(user1.getEmail().getValue(), invite1);

    val service = setupListUsersTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUsersRequest(programName);
    val invitations = new ArrayList<JoinProgramInviteEntity>(pendingInvitations);
    invitations.add(invite1);
    invite2.setStatus(null);
    invite2.setAcceptedAt(null);
    invitations.add(invite2);

    val expected = programConverter.invitationsToListUsersResponse(invitations);
    val responseObserver = mock(StreamObserver.class);

    service.listUsers(request, responseObserver);
    val argument = ArgumentCaptor.forClass(ListUsersResponse.class);

    verify(responseObserver).onNext(argument.capture());
    val actualInvitations = Set.copyOf(argument.getValue().getUserDetailsList());
    val expectedInvitations = Set.copyOf(expected.getUserDetailsList());

    assertTrue(actualInvitations.containsAll(expectedInvitations));
    assertTrue(expectedInvitations.containsAll(actualInvitations));
  }

  @Test
  void listUsers3() {
    // Case 3: An ego user who has pending status in the database.
    val programName = "TEST-CA";
    val program = mockProgram(programName);

    val invite = createPendingInvitation(program);
    val pendingInvitations = List.of(invite);

    val user = fromInvite(invite);
    val egoUsers = List.of(user);

    val egoInvitations = new TreeMap<String, JoinProgramInviteEntity>();
    egoInvitations.put(user.getEmail().getValue(), invite);

    val service = setupListUsersTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUsersRequest(programName);
    val invitations = new ArrayList<JoinProgramInviteEntity>(pendingInvitations);

    val expected = programConverter.invitationsToListUsersResponse(invitations);
    val responseObserver = mock(StreamObserver.class);

    service.listUsers(request, responseObserver);
    val argument = ArgumentCaptor.forClass(ListUsersResponse.class);

    verify(responseObserver).onNext(argument.capture());
    val actualInvitationList = argument.getValue().getUserDetailsList();
    // Ensure that we haven't listed the same user twice
    assertEquals(1, actualInvitationList.size());
    val actualInvite = actualInvitationList.get(0);
    val expectedInvite = expected.getUserDetailsList().get(0);
    assertEquals(expectedInvite, actualInvite);
  }

  @Test
  void listUsers4() {
    // Case 4: An ego user who is not in the database.
    // Make sure the user's status and accepted date is null.

    val programName = "TEST-CA";
    val program = mockProgram(programName);

    val invite = createInvitationWithStatus(program, JoinProgramInviteEntity.Status.REVOKED);
    val pendingInvitations = new ArrayList<JoinProgramInviteEntity>();

    val user = fromInvite(invite);
    val egoUsers = List.of(user);
    val egoInvitations = new TreeMap<@Email String, JoinProgramInviteEntity>();

    val service = setupListUsersTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUsersRequest(programName);
    val expectedUserDetails = createUserDetails(user, null, null);

    val responseObserver = mock(StreamObserver.class);

    service.listUsers(request, responseObserver);
    val argument = ArgumentCaptor.forClass(ListUsersResponse.class);

    verify(responseObserver).onNext(argument.capture());
    val actual = Set.copyOf(argument.getValue().getUserDetailsList());
    val expected = Set.of(expectedUserDetails);

    // Ensure that we haven't listed the same user twice
    assertEquals(1, argument.getValue().getUserDetailsList().size());

    assertTrue(actual.containsAll(expected));
    assertTrue(expected.containsAll(actual));
  }

  @Test
  void listUsers5() {
    // Case 5: An ego user who is pending in the database.
    // Make sure the user's status and accepted date is null.

    val programName = "TEST-CA";
    val program = mockProgram(programName);

    val invite = createInvitationWithStatus(program, JoinProgramInviteEntity.Status.REVOKED);
    val pendingInvitations = new ArrayList<JoinProgramInviteEntity>();

    val user = fromInvite(invite);
    val egoUsers = List.of(user);
    val egoInvitations = new TreeMap<@Email String, JoinProgramInviteEntity>();

    val service = setupListUsersTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUsersRequest(programName);
    val expectedUserDetails = createUserDetails(user, null, null);

    @SuppressWarnings("unchecked")
    val responseObserver = (StreamObserver<ListUsersResponse>) mock(StreamObserver.class);

    service.listUsers(request, responseObserver);
    val argument = ArgumentCaptor.forClass(ListUsersResponse.class);

    verify(responseObserver).onNext(argument.capture());
    val actual = Set.copyOf(argument.getValue().getUserDetailsList());
    val expected = Set.of(expectedUserDetails);

    // Ensure that we haven't listed the same user twice
    assertEquals(1, argument.getValue().getUserDetailsList().size());

    assertTrue(actual.containsAll(expected));
    assertTrue(expected.containsAll(actual));
  }

  @Test
  void testUser6() {
    // Case 6: Only the latest invitation should be retrieved for a user with multiple invitations.
    // All revoked or invalid invitations should not be retrieved.
    val programName = "TEST-CA";
    val program = mockProgram(programName);

    val pendingInvitations = List.of(createPendingInvitation(program),
      createPendingInvitation(program));

    val invite1 = createInvitation(program);
    val invite2 = createInvitation(program);
    val user1 = fromInvite(invite1);
    val user2 = fromInvite(invite2);
    val egoUsers = List.of(user1, user2);

    val egoInvitations = new TreeMap<String, JoinProgramInviteEntity>();
    egoInvitations.put(user1.getEmail().getValue(), invite1);

    val service = setupListUsersTest("TEST-CA", pendingInvitations, egoUsers,
      egoInvitations);

    val request = createListUsersRequest(programName);
    val invitations = new ArrayList<JoinProgramInviteEntity>(pendingInvitations);
    invitations.add(invite1);
    invite2.setStatus(null);
    invite2.setAcceptedAt(null);
    invitations.add(invite2);

    val expected = programConverter.invitationsToListUsersResponse(invitations);
    val responseObserver = mock(StreamObserver.class);

    service.listUsers(request, responseObserver);
    val argument = ArgumentCaptor.forClass(ListUsersResponse.class);

    verify(responseObserver).onNext(argument.capture());
    val actualInvitations = Set.copyOf(argument.getValue().getUserDetailsList());
    val expectedInvitations = Set.copyOf(expected.getUserDetailsList());

    assertTrue(actualInvitations.containsAll(expectedInvitations));
    assertTrue(expectedInvitations.containsAll(actualInvitations));
  }


  @Test
  void testInvitationExpiry() {
    val programName = "TEST-CA";
    val program = mockProgram(programName);
    val invite = createPendingInvitation(program);
    assertFalse(invite.isExpired());
    assertEquals(JoinProgramInviteEntity.Status.PENDING, invite.getStatus());
    invite.setExpiresAt(LocalDateTime.now().minusDays(1));
    assertTrue(invite.isExpired());
    assertEquals(JoinProgramInviteEntity.Status.EXPIRED, invite.getStatus());
  }

  void getInvite() {
    val randomUUID = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    val responseObserver = (StreamObserver<GetJoinProgramInviteResponse>) mock(StreamObserver.class);
    val request = mock(GetJoinProgramInviteRequest.class);
    when(request.getInviteId()).thenReturn(StringValue.newBuilder().setValue(randomUUID.toString()).build());

    val joinProgramInvite = mock(JoinProgramInviteEntity.class);
    when(joinProgramInvite.getId()).thenReturn(randomUUID);
    when(joinProgramInvite.getRole()).thenReturn(UserRole.ADMIN);
    when(joinProgramInvite.getUserEmail()).thenReturn("admin@example.com");
    when(joinProgramInvite.getFirstName()).thenReturn("First");
    when(joinProgramInvite.getLastName()).thenReturn("Last");

    when(invitationService.getLatestInvitation(any(), any())).thenReturn(Optional.of(joinProgramInvite));
    programServiceImpl.getJoinProgramInvite(request, responseObserver);

    val respArg = ArgumentCaptor.forClass(GetJoinProgramInviteResponse.class);
    verify(responseObserver).onNext(respArg.capture());
    assertEquals("Should return an response whose invitation's id is the randomUUID",
      respArg.getValue().getInvitation().getId().getValue(), randomUUID.toString());

  }

  User fromInvite(JoinProgramInviteEntity invite) {
    val roleValue = UserRoleValue.newBuilder().setValue(invite.getRole()).build();

    return User.newBuilder().
      setEmail(StringValue.of(invite.getUserEmail())).
      setFirstName(StringValue.of(invite.getFirstName())).
      setLastName(StringValue.of(invite.getLastName())).
      setRole(roleValue).
      build();
  }

  ProgramServiceImpl setupListUsersTest(
    String programName,
    List<JoinProgramInviteEntity> pendingInvitations,
    List<User> egoUsers,
    Map<String, JoinProgramInviteEntity> egoInvitations) {
    ProgramService programService = mock(ProgramService.class);
    InvitationService invitationService = mock(InvitationService.class);
    EgoService egoService = mock(EgoService.class);
    AuthorizationService authorizationService = mock(AuthorizationService.class);

    for (val key : egoInvitations.keySet()) {
      when(invitationService.getLatestInvitation(programName, key)).thenReturn(Optional.of(egoInvitations.get(key)));
    }

    when(invitationService.listPendingInvitations(programName)).thenReturn(pendingInvitations);
    when(egoService.getUsersInProgram(programName)).thenReturn(egoUsers);

    return new ProgramServiceImpl(programService, programConverter, CommonConverter.INSTANCE, egoService,
      invitationService, authorizationService, validatorFactory);
  }

  ListUsersRequest createListUsersRequest(String shortName) {
    return ListUsersRequest.newBuilder().
      setProgramShortName(StringValue.of(shortName)).build();
  }

  UserDetails createUserDetails(User user, LocalDateTime accepted, InviteStatus status) {
    val builder = UserDetails.newBuilder().setUser(user);
    if (status == null) {
      return builder.build();
    }
    val builder2 = builder.setStatus(InviteStatusValue.newBuilder().setValue(status).build());
    if (accepted == null) {
      return builder2.build();
    }
    return builder2.setAcceptedAt(CommonConverter.INSTANCE.localDateTimeToTimestamp(accepted)).build();
  }

  ProgramEntity mockProgram(String shortName) {
    val program = mock(ProgramEntity.class);
    when(programService.getProgram(shortName)).thenReturn(program);

    return program;
  }

  JoinProgramInviteEntity createInvitationWithStatus(ProgramEntity program,
    JoinProgramInviteEntity.Status status) {
    val created = LocalDateTime.now();

    val firstName = RandomStringUtils.randomAlphabetic(5);
    val lastName = RandomStringUtils.randomAlphabetic(10);

    val invite = new JoinProgramInviteEntity().
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
    if (status == JoinProgramInviteEntity.Status.PENDING) {
      return invite;
    }
    val accepted = created.plusDays(nextInt(90));
    return invite.setAcceptedAt(accepted);
  }

  JoinProgramInviteEntity createPendingInvitation(ProgramEntity program) {
    return createInvitationWithStatus(program, JoinProgramInviteEntity.Status.PENDING);
  }

  JoinProgramInviteEntity createInvitation(ProgramEntity program) {
    return createInvitationWithStatus(program,
      RandomChoiceFrom(JoinProgramInviteEntity.Status.values()));
  }

  <T> T RandomChoiceFrom(T[] elements) {
    return elements[nextInt(elements.length - 1)];
  }

}