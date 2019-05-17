package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.*;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.mappers.ProgramMapper;
import org.icgc.argo.program_service.services.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {
  private final ProgramService programService;
  private final ProgramMapper programMapper;

  @Autowired
  public ProgramServiceImpl(ProgramService programService, ProgramMapper programMapper) {
    this.programMapper = programMapper;
      this.programService = programService;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    val program = request.getProgram();

    val result = programService.createProgram(program);
    
    if (result.hasError()) {
      responseObserver.onError(result.getError());
      responseObserver.onCompleted();
      return;
    }

    val programEntity = result.getValue();
    val response = CreateProgramResponse
      .newBuilder()
      .setId(programMapper.map(programEntity.getId()))
      .setCreatedAt(programMapper.map(programEntity.getCreatedAt()))
      .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void inviteUser(InviteUserRequest request, StreamObserver<InviteUserResponse> responseObserver) {
    val programId = java.util.UUID.fromString(request.getProgramId());
    val program = programService.getProgram(programId);

    if (program.isEmpty()) {
      responseObserver.onError(new StatusException(Status.fromCode(Status.Code.NOT_FOUND)));
      return;
    }

    val id = programService.inviteUser(program.get(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getRole());
    val inviteUserResponse = InviteUserResponse.newBuilder().setInviteId(id.toString()).build();
    responseObserver.onNext(inviteUserResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val programs = programService.listPrograms();

    val collection = ListProgramsResponse
            .newBuilder()
            .addAllPrograms(programs)
            .build();

    responseObserver.onNext(collection);
    responseObserver.onCompleted();
  }


  @Override
  public void removeUser(org.icgc.argo.program_service.RemoveUserRequest request,
                         io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }
}
