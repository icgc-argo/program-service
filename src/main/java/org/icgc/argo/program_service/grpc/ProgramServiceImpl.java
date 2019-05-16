package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.CreateProgramRequest;
import org.icgc.argo.program_service.CreateProgramResponse;
import org.icgc.argo.program_service.InviteUserRequest;
import org.icgc.argo.program_service.InviteUserResponse;
import org.icgc.argo.program_service.ListProgramsResponse;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.converter.FromProtoProgramConverter;
import org.icgc.argo.program_service.converter.ToProtoProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.services.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  /**
   * Dependencies
   */
  private final ProgramService programService;
  private final ToProtoProgramConverter toProtoProgramConverter;
  private final FromProtoProgramConverter fromProtoProgramConverter;

  @Autowired
  public ProgramServiceImpl(@NonNull ProgramService programService,
     @NonNull ToProtoProgramConverter toProtoProgramConverter,
      @NonNull FromProtoProgramConverter fromProtoProgramConverter) {
      this.programService = programService;
    this.toProtoProgramConverter = toProtoProgramConverter;
    this.fromProtoProgramConverter = fromProtoProgramConverter;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    val program = request.getProgram();
    val entity = programService.createProgram(program);
    val response = toProtoProgramConverter.programEntityToCreateProgramResponse(entity);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void inviteUser(InviteUserRequest request, StreamObserver<InviteUserResponse> responseObserver) {
    val programId = java.util.UUID.fromString(request.getProgramId().getValue());
    val programResult = programService.getProgram(programId);

    if (programResult.isEmpty()) {
      responseObserver.onError(new StatusException(Status.fromCode(Status.Code.NOT_FOUND)));
      return;
    }

    val inviteId = programService.inviteUser(programResult.get(),
        request.getEmail().getValue(),
        request.getFirstName().getValue(),
        request.getLastName().getValue(),
        request.getRole().getValue());

    val inviteUserResponse = toProtoProgramConverter.inviteIdToInviteUserResponse(inviteId);
    responseObserver.onNext(inviteUserResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val programEntities = programService.listPrograms();
    val listProgramsResponse = toProtoProgramConverter.programEntitiesToListProgramsResponse(programEntities);
    responseObserver.onNext(listProgramsResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void removeUser(org.icgc.argo.program_service.RemoveUserRequest request,
                         io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }
}
