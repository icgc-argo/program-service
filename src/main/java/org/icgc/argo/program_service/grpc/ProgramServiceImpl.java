package org.icgc.argo.program_service.grpc;

import lombok.extern.slf4j.Slf4j;
import org.icgc.argo.program_service.ProgramDetails;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.grpc.EgoAuthInterceptor.EgoAuth;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void create(ProgramDetails request,
                     io.grpc.stub.StreamObserver<ProgramDetails> responseObserver) {
    ProgramDetails detail = ProgramDetails.newBuilder().build();
//    EgoAuthInterceptor.EGO_TOKEN.get();
    responseObserver.onNext(detail);
    responseObserver.onCompleted();
  }
}


