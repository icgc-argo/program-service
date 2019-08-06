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

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.AuthorizationService;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.grpc.Status.NOT_FOUND;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToList;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToSet;

@Slf4j
@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  /**
   * Dependencies
   */
  private final ProgramService programService;
  private final InvitationService invitationService;
  private final ProgramConverter programConverter;
  private final CommonConverter commonConverter;
  private final EgoService egoService;
  private final AuthorizationService authorizationService;

  @Autowired
  public ProgramServiceImpl(@NonNull ProgramService programService, @NonNull ProgramConverter programConverter,
    @NonNull CommonConverter commonConverter, @NonNull EgoService egoService,InvitationService invitationService,
    AuthorizationService authorizationService) {
    this.programService = programService;
    this.programConverter = programConverter;
    this.egoService = egoService;
    this.commonConverter = commonConverter;
    this.invitationService = invitationService;
    this.authorizationService = authorizationService;
  }


  //TODO: need better error response. If a duplicate program is created, get "2 UNKNOWN" error. Should atleast have a message in it
  @Override
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    authorizationService.requireDCCAdmin();

    val program = request.getProgram();
    val admins = request.getAdminsList();

    val programEntity = programService.createWithSideEffectTransactional(program, (ProgramEntity pe) -> {
      egoService.setUpProgram(pe.getShortName());
      admins.forEach(admin -> {
        val email = commonConverter.unboxStringValue(admin.getEmail());
        val firstName = commonConverter.unboxStringValue(admin.getFirstName());
        val lastName = commonConverter.unboxStringValue(admin.getLastName());
        egoService.getOrCreateUser(email, firstName, lastName);
        invitationService.inviteUser(pe, email, firstName, lastName, UserRole.ADMIN);
        });
    });

    val response = programConverter.programEntityToCreateProgramResponse(programEntity);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  @Transactional
  public void getProgram(GetProgramRequest request, StreamObserver<GetProgramResponse> responseObserver) {
    val shortName = request.getShortName().getValue();
    authorizationService.requireProgramUser(shortName);

    val programEntity = programService.getProgram(shortName);
    val programDetails = programConverter.ProgramEntityToProgramDetails(programEntity);
    val response = GetProgramResponse.newBuilder().setProgram(programDetails).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private StatusRuntimeException status(Status code, String message) {
    return code.
      augmentDescription(message).
      asRuntimeException();
  }

  private StatusRuntimeException status(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException) {
      return (StatusRuntimeException) throwable;
    }

    return Status.UNKNOWN.
      withCause(throwable).
      augmentDescription(throwable.getMessage()).
      asRuntimeException();
  }

  @Override
  public void updateProgram(UpdateProgramRequest request, StreamObserver<UpdateProgramResponse> responseObserver) {
    authorizationService.requireDCCAdmin();

    val program = request.getProgram();
    val updatingProgram = programConverter.programToProgramEntity(program);
    try {
      val updatedProgram = programService.updateProgram(
              updatingProgram,
              program.getCancerTypesList(),
              program.getPrimarySitesList(),
              program.getInstitutionsList(),
              program.getCountriesList(),
              program.getRegionsList()
      );
      val response = programConverter.programEntityToUpdateProgramResponse(updatedProgram);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException | NoSuchElementException e) {
      log.error("Exception throw in updateProgram: {}", e.getMessage());
      e.printStackTrace();
      throw status(NOT_FOUND, e.getMessage());
    }
  }

  @Override
  @Transactional
  public void inviteUser(InviteUserRequest request, StreamObserver<InviteUserResponse> responseObserver) {
    val programShortName = request.getProgramShortName().getValue();
    authorizationService.requireProgramAdmin(programShortName);

    val programResult = programService.getProgram(programShortName);
    UUID inviteId;

    try {
      val email = commonConverter.unboxStringValue(request.getEmail());
      val firstName = commonConverter.unboxStringValue(request.getFirstName());
      val lastName = commonConverter.unboxStringValue(request.getLastName());
      inviteId = invitationService.inviteUser(programResult, email, firstName, lastName, request.getRole().getValue());
      egoService.getOrCreateUser(email, firstName, lastName);
    } catch (Throwable throwable) {
      responseObserver.onError(status(throwable));
      return;
    }

    val inviteUserResponse = programConverter.inviteIdToInviteUserResponse(inviteId);

    responseObserver.onNext(inviteUserResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void joinProgram(JoinProgramRequest request, StreamObserver<JoinProgramResponse> responseObserver) {
    val str = request.getJoinProgramInvitationId().getValue();
    val id = commonConverter.stringToUUID(str);
    val invitation = invitationService.acceptInvite(id);

    authorizationService.requireEmail(invitation.getEmail());

    try {
      val responseUser = invitationService.acceptInvite(id);
      val response = programConverter.egoUserToJoinProgramResponse(responseUser);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException e) {
      log.error("Exception throw in joinProgram: {}", e.getMessage());
      e.printStackTrace();
      throw status(NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
      List<ProgramEntity> programEntities = programService.listPrograms().stream().
        filter(p -> authorizationService.canRead(p.getShortName())).
        collect(Collectors.toList());
    val listProgramsResponse = programConverter.programEntitiesToListProgramsResponse(programEntities);

    responseObserver.onNext(listProgramsResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
    ListUsersResponse response;
    val programShortName = request.getProgramShortName().getValue();
    authorizationService.requireProgramAdmin(programShortName);

    val users = egoService.getUsersInProgram(programShortName);
    Set<UserDetails> userDetails = mapToSet(users, user -> programConverter.userWithOptionalJoinProgramInviteToUserDetails(user,
      invitationService.getInvitation(programShortName, user.getEmail().getValue())));

    userDetails.addAll(mapToList(invitationService.listPendingInvitations(programShortName),
      programConverter::joinProgramInviteToUserDetails ));

    response = ListUsersResponse.newBuilder().addAllUserDetails(userDetails).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }


  @Override
  public void removeUser(RemoveUserRequest request, StreamObserver<RemoveUserResponse> responseObserver) {
    val programName = request.getProgramShortName().getValue();
    authorizationService.requireProgramAdmin(programName);

    val email = request.getUserEmail().getValue();
    egoService.leaveProgram(email, programName);
    val response = programConverter.toRemoveUserResponse("User is successfully removed!");
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateUser(UpdateUserRequest request, StreamObserver<Empty> responseObserver) {
    val programShortName = request.getShortName().getValue();
    authorizationService.requireProgramAdmin(programShortName);

    val email =request.getUserEmail().getValue();
    val role = request.getRole().getValue();
    
    try {
      egoService.updateUserRole(email, programShortName, role);
    } catch (NotFoundException e) {
      log.error("Exception throw in updateUser: {}", e.getMessage());
      e.printStackTrace();
      throw status(NOT_FOUND, e.getMessage());
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void removeProgram(RemoveProgramRequest request, StreamObserver<Empty> responseObserver) {
    authorizationService.requireDCCAdmin();

    val shortName = request.getProgramShortName().getValue();
    try {
      egoService.cleanUpProgram(shortName);
      programService.removeProgram(request.getProgramShortName().getValue());
    } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException e) {
      log.error("Exception throw in removeProgram: {}", e.getMessage());
      e.printStackTrace();
      throw status(NOT_FOUND, getExceptionMessage(e));
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  private String getExceptionMessage(NestedRuntimeException e) {
    return e.getMostSpecificCause().getMessage();
  }

  @Override
  @Transactional
  public void getJoinProgramInvite(GetJoinProgramInviteRequest request,
    StreamObserver<GetJoinProgramInviteResponse> responseObserver) {
    val joinProgramInvite = invitationService.getInvitation(UUID.fromString(request.getInviteId().getValue()));
    if (joinProgramInvite.isEmpty()) {
      responseObserver.onError(Status.NOT_FOUND.withDescription("Invitation is not found").asRuntimeException());
      return;
    }
    val invitation = programConverter.joinProgramInviteEntityToJoinProgramInvite(joinProgramInvite.get());
    val response = GetJoinProgramInviteResponse.newBuilder().setInvitation(invitation).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }


}
