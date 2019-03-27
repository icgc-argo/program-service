package org.icgc.argo.program_service.grpc;

import org.icgc.argo.program_service.ProgramDetails;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.springframework.stereotype.Component;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  @EgoAuthInterceptor.EgoAuth(rolesAllowed = {"ADMIN"})
  public void create(ProgramDetails request,
                     io.grpc.stub.StreamObserver<ProgramDetails> responseObserver) {
    ProgramDetails detail = ProgramDetails.newBuilder().build();
//    EgoAuthInterceptor.EGO_TOKEN.get();
    responseObserver.onNext(detail);
    responseObserver.onCompleted();
  }
}
