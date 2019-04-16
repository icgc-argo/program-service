package org.icgc.argo.program_service.grpc;

import io.grpc.ManagedChannelBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import lombok.val;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GRpcServerRunnerIT {
  @Test
  void healthCheck() {
    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();

    val blockingStub = HealthGrpc.newBlockingStub(channel);

    val emptyServiceRequest = HealthCheckRequest.newBuilder().setService("").build();
    assertEquals(blockingStub.check(emptyServiceRequest).getStatus(), HealthCheckResponse.ServingStatus.SERVING, "grpc is running");

    val programServiceRequest = HealthCheckRequest.newBuilder().setService("program_service.ProgramService").build();
    assertEquals(blockingStub.check(programServiceRequest).getStatus(), HealthCheckResponse.ServingStatus.SERVING, "programService is running");
  }
}