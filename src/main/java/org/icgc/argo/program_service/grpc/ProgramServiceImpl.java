package org.icgc.argo.program_service.grpc;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.InviteUserRequest;
import org.icgc.argo.program_service.InviteUserResponse;
import org.icgc.argo.program_service.ProgramDetails;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.icgc.argo.program_service.services.ProgramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  private final ProgramRepository programRepository;
  private final ProgramService programService;

  @Autowired
  public ProgramServiceImpl(ProgramRepository programRepository, ProgramService programService) {
    this.programRepository = programRepository;
    this.programService = programService;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void create(
      ProgramDetails request, io.grpc.stub.StreamObserver<ProgramDetails> responseObserver) {

    val programProto = request.getProgram();

    val programDao =
        Program.builder()
            .name(programProto.getName())
            .shortName(programProto.getShortName())
            .description(programProto.getDescription())
            .commitmentDonors(programProto.getCommitmentDonors())
            .genomicDonors(programProto.getGenomicDonors())
            .membershipType(programProto.getMembershipType().toString())
            .website(programProto.getWebsite())
            .submittedDonors(programProto.getSubmittedDonors())
            .dateCreated(new Date())
            .build();

    programRepository.save(programDao);
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
}
