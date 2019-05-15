package org.icgc.argo.program_service.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.val;
import net.bytebuddy.utility.RandomString;
import org.icgc.argo.program_service.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgramServiceImplClientIT {
  private ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
  private ProgramServiceGrpc.ProgramServiceBlockingStub blockingStub = ProgramServiceGrpc.newBlockingStub(channel);

  @Test
  void createAndListPrograms() {
    val program = Program.newBuilder().setName(RandomString.make(15)).setShortName(RandomString.make(10)).setDescription("nothing");

    val createProgramRequest = CreateProgramRequest.newBuilder().setProgram(program).build();

    val response = blockingStub.createProgram(createProgramRequest);
    assertThat(response.getId()).isNotEmpty();
  }

  @Test
  void joinAndLeaveProgram() {
    val program = Program.newBuilder().setName(RandomString.make(15)).setShortName(RandomString.make(10)).setDescription("nothing");
    val createProgramRequest = CreateProgramRequest.newBuilder().setProgram(program).build();
    val response = blockingStub.createProgram(createProgramRequest);
    val programId = response.getId();

    val inviteUserRequest = InviteUserRequest.newBuilder().setFirstName("First").setLastName("Last").setEmail("user@example.com").setRole(UserRole.ADMIN).setProgramId(programId).build();
    val inviteUserResponse = blockingStub.inviteUser(inviteUserRequest);

    assertThat(inviteUserResponse.getInviteId()).isNotEmpty();
  }
}
