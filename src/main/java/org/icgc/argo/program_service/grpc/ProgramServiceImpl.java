package org.icgc.argo.program_service.grpc;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.*;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.mappers.ProgramMapper;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.icgc.argo.program_service.services.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {
  private final ProgramRepository programRepository;
  private final ProgramService programService;
  private final ProgramMapper programMapper;

  @Autowired
  public ProgramServiceImpl(ProgramRepository programRepository, ProgramService programService, ProgramMapper programMapper) {
    this.programRepository = programRepository;
    this.programMapper = programMapper;
      this.programService = programService;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    val program = request.getProgram();

    val programEntity = programService.createProgram(program);
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

  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val programs = programService.listPrograms();

    val collection = ListProgramsResponse
            .newBuilder()
            .addAllPrograms(programs)
            .build();

    responseObserver.onNext(collection);
    responseObserver.onCompleted();
  }
}
