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
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static io.grpc.Status.NOT_FOUND;

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

  @Autowired
  public ProgramServiceImpl(@NonNull ProgramService programService, @NonNull ProgramConverter programConverter,
    @NonNull CommonConverter commonConverter, @NonNull EgoService egoService,InvitationService invitationService) {
    this.programService = programService;
    this.programConverter = programConverter;
    this.egoService = egoService;
    this.commonConverter = commonConverter;
    this.invitationService = invitationService;
  }

  //TODO: need better error response. If a duplicate program is created, get "2 UNKNOWN" error. Should atleast have a message in it
  @Override
  @EgoAuth(typesAllowed = { "ADMIN" })
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    val program = request.getProgram();
    val admins = request.getAdminsList();

    val programEntity = programService.createProgram(program);
    egoService.setUpProgram(programEntity.getShortName());
    admins.forEach(admin -> {
              val email = commonConverter.unboxStringValue(admin.getEmail());
              val firstName = commonConverter.unboxStringValue(admin.getFirstName());
              val lastName = commonConverter.unboxStringValue(admin.getLastName());
              egoService.getOrCreateUser(email, firstName, lastName);
              invitationService.inviteUser(programEntity, email, firstName, lastName, UserRole.ADMIN);
    });

    val response = programConverter.programEntityToCreateProgramResponse(programEntity);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getProgram(GetProgramRequest request, StreamObserver<GetProgramResponse> responseObserver) {
    val shortName = request.getShortName().getValue();
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
  @EgoAuth(typesAllowed = { "ADMIN" })
  public void updateProgram(UpdateProgramRequest request, StreamObserver<UpdateProgramResponse> responseObserver) {
    val program = request.getProgram();
    val updatingProgram = programConverter.programToProgramEntity(program);
    try {
      val updatedProgram = programService.updateProgram(updatingProgram, program.getCancerTypesList(), program.getPrimarySitesList());
      val response = programConverter.programEntityToUpdateProgramResponse(updatedProgram);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException | NoSuchElementException e) {
        throw status(NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public void inviteUser(InviteUserRequest request, StreamObserver<InviteUserResponse> responseObserver) {
    val programResult = programService.getProgram(request.getProgramShortName().getValue());
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
    try {
      val responseUser = invitationService.acceptInvite(commonConverter.stringToUUID(request.getJoinProgramInvitationId()));
      val response = programConverter.egoUserToJoinProgramResponse(responseUser);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException e) {
      throw status(NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val programEntities = programService.listPrograms();

    val listProgramsResponse = programConverter.programEntitiesToListProgramsResponse(programEntities);

    responseObserver.onNext(listProgramsResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listUser(ListUserRequest request, StreamObserver<ListUserResponse> responseObserver) {
    ListUserResponse response;

    try {
      val shortName = request.getProgramShortName().getValue();
      val invitations = invitationService.listInvitations(shortName);

      response = programConverter.invitationsToListUserResponse(invitations);
    } catch (Throwable t) {
      responseObserver.onError(status(t));
      return;
    }

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void removeUser(RemoveUserRequest request, StreamObserver<RemoveUserResponse> responseObserver) {
    val programName = request.getProgramShortName().getValue();
    val email = request.getUserEmail().getValue();
    egoService.leaveProgram(email, programName);
    val response = programConverter.toRemoveUserResponse("User is successfully removed!");
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateUser(UpdateUserRequest request, StreamObserver<Empty> responseObserver) {
    val userId = commonConverter.stringToUUID(request.getUserId());
    val role = request.getRole().getValue();
    val shortname = commonConverter.unboxStringValue(request.getShortName());
    try {
      egoService.updateUserRole(userId, shortname, role);
    } catch (NotFoundException e) {
      throw status(NOT_FOUND, e.getMessage());
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void removeProgram(RemoveProgramRequest request, StreamObserver<Empty> responseObserver) {
    val shortName = request.getProgramShortName().getValue();
    try {
      egoService.cleanUpProgram(shortName);
      programService.removeProgram(request.getProgramShortName().getValue());
    } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException e) {
      throw status(NOT_FOUND, getExceptionMessage(e));
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  private String getExceptionMessage(NestedRuntimeException e) {
    return e.getMostSpecificCause().getMessage();
  }
}