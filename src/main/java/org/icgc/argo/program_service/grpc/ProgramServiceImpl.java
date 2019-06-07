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
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ego.model.exceptions.EgoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Component;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  /**
   * Dependencies
   */
  private final ProgramService programService;
  private final ProgramConverter programConverter;
  private final CommonConverter commonConverter;
  private final EgoService egoService;

  @Autowired
  public ProgramServiceImpl(@NonNull ProgramService programService, @NonNull ProgramConverter programConverter,
                            @NonNull CommonConverter commonConverter, @NonNull EgoService egoService ) {
    this.programService = programService;
    this.programConverter = programConverter;
    this.egoService = egoService;
    this.commonConverter = commonConverter;
  }


  //TODO: need better error response. If a duplicate program is created, get "2 UNKNOWN" error. Should atleast have a message in it
  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    val program = request.getProgram();
    ProgramEntity entity;

    try {
      entity = programService.createProgram(program, request.getAdminEmailsList());
    } catch (DataIntegrityViolationException e) {
      responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(getExceptionMessage(e)).asRuntimeException());
      return;
    } catch (EgoException egoException) {
      responseObserver.onError(egoException);
      return;
    }

    val response = programConverter.programEntityToCreateProgramResponse(entity);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void updateProgram(UpdateProgramRequest request, StreamObserver<UpdateProgramResponse> responseObserver){
    val program = request.getProgram();
    val updatedProgram = programService.updateProgram(program);
    val response = programConverter.programEntityToUpdateProgramResponse(updatedProgram);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void inviteUser(InviteUserRequest request, StreamObserver<InviteUserResponse> responseObserver) {
    val programId = commonConverter.stringToUUID(request.getProgramId());
    val programResult = programService.getProgram(programId);

    if (programResult.isEmpty()) {
      responseObserver.onError(Status.NOT_FOUND.withDescription(String.format("Cannot find program with programId: %s", programId.toString())).asRuntimeException());
      return;
    }

    val inviteId = programService.inviteUser(programResult.get(),
        request.getEmail().getValue(),
        request.getFirstName().getValue(),
        request.getLastName().getValue(),
        request.getRole().getValue());

    val inviteUserResponse = programConverter.inviteIdToInviteUserResponse(inviteId);
    responseObserver.onNext(inviteUserResponse);
    responseObserver.onCompleted();
  }

  // not tested
  @Override
  public void joinProgram(JoinProgramRequest request,
                          StreamObserver<com.google.protobuf.Empty> responseObserver) {
    val succeed = programService.acceptInvite(commonConverter.stringToUUID(request.getJoinProgramInvitationId()));
    if (!succeed) {
      responseObserver.onError(new StatusException(Status.fromCode(Status.Code.UNKNOWN)));
      return;
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
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
    val programId = request.getProgramId();
    val users = programService.listUser(commonConverter.stringToUUID(programId));
    val response = programConverter.usersToListUserResponse(users);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  // not tested
  @Override
  public void removeUser(RemoveUserRequest request,
                         StreamObserver<com.google.protobuf.Empty> responseObserver) {
    egoService.leaveProgram(commonConverter.stringToUUID(request.getUserId()), commonConverter.stringToUUID(request.getProgramId()));
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void removeProgram(RemoveProgramRequest request, StreamObserver<Empty> responseObserver) {
    try {
      programService.removeProgram(commonConverter.stringToUUID(request.getProgramId()));
    } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException e) {
      responseObserver.onError(Status.NOT_FOUND.withDescription(getExceptionMessage(e)).asRuntimeException());
      return;
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  private String getExceptionMessage(NestedRuntimeException e) {
    return e.getMostSpecificCause().getMessage();
  }
}

