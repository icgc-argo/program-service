package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannelBuilder;
import lombok.val;
import net.bytebuddy.utility.RandomString;
import org.icgc.argo.program_service.CreateProgramRequest;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramServiceClientIT {

  @Test
  void listPrograms() {
    val channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();

    val blockingStub = ProgramServiceGrpc.newBlockingStub(channel);

    val instant = Instant.now();
    val timestamp = Timestamp.newBuilder().setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();

    val program = Program.newBuilder().setName(RandomString.make(15)).setCreatedAt(timestamp).setShortName(RandomString.make(10)).setDescription("nothing");

    val createProgramRequest = CreateProgramRequest.newBuilder().setProgram(program).build();

    val response = blockingStub.createProgram(createProgramRequest);
    assertThat(response.getId()).isNotEmpty();
  }
}
