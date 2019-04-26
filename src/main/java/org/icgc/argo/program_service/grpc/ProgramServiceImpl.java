package org.icgc.argo.program_service.grpc;

import com.google.common.collect.Streams;
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

import java.util.stream.Collectors;

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
  public void create(ProgramDetails request, StreamObserver<ProgramDetails> responseObserver) {
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    val program = request.getProgram();
    val dao = programMapper.ProgramMessageToProgram(program);
    programRepository.save(dao);
    responseObserver.onNext(request);
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

    programService.inviteUser(program.get(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getRole());
    responseObserver.onCompleted();
  }

  public void list(Empty request, StreamObserver<ProgramCollection> responseObserver) {
    val programs = programRepository.findAll();
    val results = Streams.stream(programs)
      .map(programMapper::ProgramToProgramMessage)
      .collect(Collectors.toUnmodifiableList());

    val collection = ProgramCollection
      .newBuilder()
      .addAllPrograms(results)
      .build();

    responseObserver.onNext(collection);
    responseObserver.onCompleted();
  }
}
