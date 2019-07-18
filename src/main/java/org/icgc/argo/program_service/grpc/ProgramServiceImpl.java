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

import java.awt.desktop.UserSessionEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToList;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToSet;

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

    ProgramEntity programEntity;

    try {
      programEntity = programService.createProgram(program);
    } catch (DataIntegrityViolationException e) {
      responseObserver.onError(
        status(Status.INVALID_ARGUMENT,
          getExceptionMessage(e)));
      return;
    }

    try {
      egoService.setUpProgram(programEntity.getShortName());
      admins.forEach(admin -> {
              val email = commonConverter.unboxStringValue(admin.getEmail());
              val firstName = commonConverter.unboxStringValue(admin.getFirstName());
              val lastName = commonConverter.unboxStringValue(admin.getLastName());
              egoService.getOrCreateUser(email, firstName, lastName);
              invitationService.inviteUser(programEntity, email, firstName, lastName, UserRole.ADMIN);
      });
    } catch (Throwable t) {
      responseObserver.onError(status(t));
      return;
    }

    val response = programConverter.programEntityToCreateProgramResponse(programEntity);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getProgram(GetProgramRequest request, StreamObserver<GetProgramResponse> responseObserver) {
    ProgramEntity programEntity;
    val shortName = request.getShortName().getValue();

    try {
      programEntity = programService.getProgram(shortName);
    } catch (Throwable t) {
      responseObserver.onError(status(t));
      return;
    }

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
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
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
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e1){
      responseObserver.onError(Status.UNKNOWN.withDescription(e1.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    List<ProgramEntity> programEntities;
    try {
      programEntities = programService.listPrograms();
    } catch (Throwable t) {
      responseObserver.onError(t);
      return;
    }

    val listProgramsResponse = programConverter.programEntitiesToListProgramsResponse(programEntities);

    responseObserver.onNext(listProgramsResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listUser(ListUserRequest request, StreamObserver<ListUserResponse> responseObserver) {
    ListUserResponse response;
    Set<Invitation> status;

    try {
      val programShortName = request.getProgramShortName().getValue();
      val users = egoService.getUsersInProgram(programShortName);
      status = mapToSet(users, user -> getInvitationForEgoUser(programShortName, user));

      status.addAll(mapToList(invitationService.listPendingInvitations(programShortName),
        programConverter::joinProgramInviteToInvitation ));

      response = ListUserResponse.newBuilder().addAllInvitations(status).build();
    } catch (Throwable t) {
      responseObserver.onError(status(t));
      return;
    }

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private Invitation getInvitationForEgoUser(String programShortName, User user) {
    val builder = Invitation.newBuilder().setUser(user);

    val invite = invitationService.getInvitation(programShortName, user.getEmail().getValue());

    if (invite.isEmpty()) {
      return builder.build();
    }

    val status = programConverter.JoinProgramInviteStatusToInviteStatus(invite.get().getStatus());
    val builder2 = builder.setStatus(programConverter.boxInviteStatus(status));

    if (status == InviteStatus.PENDING) {
      return builder2.build();
    }

    val accepted = CommonConverter.INSTANCE.localDateTimeToTimestamp(invite.get().getAcceptedAt());
    return builder2.setAcceptedAt(accepted).build();
  }

  @Override
  public void removeUser(RemoveUserRequest request, StreamObserver<RemoveUserResponse> responseObserver) {
    val programName = request.getProgramShortName().getValue();
    val email = request.getUserEmail().getValue();
    try {
      egoService.leaveProgram(email, programName);
    } catch (Throwable throwable) {
      responseObserver.onError(status(throwable));
      return;
    }
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
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch (RuntimeException e) {
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
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
      responseObserver.onError(Status.NOT_FOUND.withDescription(getExceptionMessage(e)).asRuntimeException());
      return;
    } catch (Throwable t) {
      responseObserver.onError(t);
      return;
    }

    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  private String getExceptionMessage(NestedRuntimeException e) {
    return e.getMostSpecificCause().getMessage();
  }
}