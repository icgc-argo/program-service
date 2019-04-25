package org.icgc.argo.program_service.grpc;

import lombok.val;
import org.icgc.argo.program_service.ProgramDetails;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.model.entity.Program;
import org.icgc.argo.program_service.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  private final ProgramRepository programRepository;

  @Autowired
  public ProgramServiceImpl(ProgramRepository programRepository) {
    this.programRepository = programRepository;
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
            .membershipType(programProto.getMembershipType())
            .website(programProto.getWebsite())
            .submittedDonors(programProto.getSubmittedDonors())
            .dateCreated(new Date())
            .build();

    programRepository.save(programDao);
    responseObserver.onNext(request);
    responseObserver.onCompleted();
  }
}
