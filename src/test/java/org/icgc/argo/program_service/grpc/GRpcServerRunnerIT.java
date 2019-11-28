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

import static org.junit.jupiter.api.Assertions.*;

import io.grpc.ManagedChannelBuilder;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import lombok.val;
import org.junit.jupiter.api.Test;

class GRpcServerRunnerIT {
  @Test
  void healthCheck() {
    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();

    val blockingStub = HealthGrpc.newBlockingStub(channel);

    val emptyServiceRequest = HealthCheckRequest.newBuilder().setService("").build();
    assertEquals(
        blockingStub.check(emptyServiceRequest).getStatus(),
        HealthCheckResponse.ServingStatus.SERVING,
        "grpc is running");

    val programServiceRequest =
        HealthCheckRequest.newBuilder().setService("program_service.ProgramService").build();
    assertEquals(
        blockingStub.check(programServiceRequest).getStatus(),
        HealthCheckResponse.ServingStatus.SERVING,
        "programService is running");
  }
}
