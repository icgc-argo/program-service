package org.icgc.argo.program_service.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.val;
import net.bytebuddy.utility.RandomString;
import org.icgc.argo.program_service.CreateProgramRequest;
import org.icgc.argo.program_service.InviteUserRequest;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.RemoveUserRequest;
import org.icgc.argo.program_service.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.UtilsTest.stringValue;
import static org.icgc.argo.program_service.UtilsTest.userRoleValue;

class ProgramServiceImplClientIT {
  private ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
  private ProgramServiceGrpc.ProgramServiceBlockingStub blockingStub = ProgramServiceGrpc.newBlockingStub(channel);

  @Test
  void createAndListPrograms() {
    val program = Program.newBuilder()
        .setName(stringValue(RandomString.make(15)))
        .setShortName(stringValue(RandomString.make(10)))
        .setDescription(stringValue("nothing"));

    val createProgramRequest = CreateProgramRequest.newBuilder().setProgram(program).build();

    val response = blockingStub.createProgram(createProgramRequest);
    assertThat(response.getId().getValue()).isNotEmpty();
  }

  @Test
  void joinAndLeaveProgram() {
    val program = Program.newBuilder().setName(RandomString.make(15)).setShortName(RandomString.make(10)).setDescription("nothing");
    val createProgramRequest = CreateProgramRequest.newBuilder().setProgram(program).build();
    val response = blockingStub.createProgram(createProgramRequest);
    val programId = response.getId();

    val inviteUserRequest = InviteUserRequest.newBuilder()
        .setFirstName(stringValue("First"))
        .setLastName(stringValue("Last"))
        .setEmail(stringValue("user@example.com"))
        .setRole(userRoleValue(UserRole.ADMIN))
        .setProgramId(programId)
        .build();
    val inviteUserResponse = blockingStub.inviteUser(inviteUserRequest);
    assertThat(inviteUserResponse.getInviteId()).isNotEmpty();
  }


}
