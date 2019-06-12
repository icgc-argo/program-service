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
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.EgoService;
import org.icgc.argo.program_service.services.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    val adminEmails = request.getAdminEmailsList();
    ProgramEntity programEntity;

    try {
      programEntity = programService.createProgram(program, adminEmails);
    } catch (DataIntegrityViolationException e) {
      responseObserver.onError(
        status(Status.INVALID_ARGUMENT,
          getExceptionMessage(e)));
      return;
    }

    try {
      egoService.setUpProgram(programEntity, adminEmails);
    } catch (EgoService.EgoException egoException) {
      responseObserver.onError(status(egoException));
      return;
    }

    val response = programConverter.programEntityToCreateProgramResponse(programEntity);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getProgram(GetProgramRequest request,  StreamObserver<GetProgramResponse> responseObserver) {
    ProgramEntity programEntity;
    val shortName=request.getShortName().getValue();

    try {
       programEntity = programService.getProgram(shortName);
    } catch(Throwable t) {
      responseObserver.onError(status(t));
      return;
    }

    val program = programConverter.programEntityToProgram(programEntity);

    val programDetails = ProgramDetails.newBuilder().
      setProgram(program).
      setMetadata(programConverter.programEntityToMetadata(programEntity)).
      build();

    val response = GetProgramResponse.newBuilder().setProgram(programDetails).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  StatusRuntimeException status(Status code, String message) {
    return code.
      augmentDescription(message).
      asRuntimeException();
  }

  StatusRuntimeException status(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException)  {
      return (StatusRuntimeException) throwable;
    }

    return Status.UNKNOWN.
      withCause(throwable).
      augmentDescription(throwable.getMessage()).
      asRuntimeException();
  }
  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void updateProgram(UpdateProgramRequest request, StreamObserver<UpdateProgramResponse> responseObserver){
     val updatingProgram = programConverter.updateProgramRequestToProgramEntity(request);
    try {
      val updatedProgram = programService.updateProgram(updatingProgram);
      val response = programConverter.programEntityToUpdateProgramResponse(updatedProgram);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException | EmptyResultDataAccessException e){
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    } catch(RuntimeException e){
      responseObserver.onError(Status.UNKNOWN.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void inviteUser(InviteUserRequest request, StreamObserver<InviteUserResponse> responseObserver) { val programName = request.getProgramShortName().getValue();
    InviteUserResponse inviteUserResponse;
    try {
      val program = programService.getProgram(programName);
      val inviteId = programService.inviteUser(program,
        request.getEmail().getValue(),
        request.getFirstName().getValue(),
        request.getLastName().getValue(),
        request.getRole().getValue());
      inviteUserResponse = programConverter.inviteIdToInviteUserResponse(inviteId);
    } catch (Throwable throwable) {
      responseObserver.onError(status(throwable));
      return;
    }
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
    val shortName = request.getProgramShortName().getValue();
    ProgramEntity programEntity;

    try {
      programEntity = programService.getProgram(shortName);
    } catch(Throwable t) {
      responseObserver.onError(status(t));
      return;
    }

    val programId = programEntity.getId();
    List<User> users;

    try {
      users = egoService.getUserByGroup(programId);
    } catch (EgoService.EgoException egoExeption) {
      responseObserver.onError(status(egoExeption));
      return;
    }
    val response = programConverter.usersToListUserResponse(users);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  // not tested
  @Override
  public void removeUser(RemoveUserRequest request,
                         StreamObserver<com.google.protobuf.Empty> responseObserver) {
    val programName = request.getProgramShortName().getValue();
    val email       = request.getUserEmail().getValue();

    try {
      val program_id = programService.getProgram(programName).getId();
      egoService.leaveProgram(email, program_id);
    } catch(Throwable throwable) {
      responseObserver.onError(status(throwable));
      return;
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void removeProgram(RemoveProgramRequest request, StreamObserver<Empty> responseObserver) {
    try {
      programService.removeProgram(request.getProgramShortName().getValue());
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