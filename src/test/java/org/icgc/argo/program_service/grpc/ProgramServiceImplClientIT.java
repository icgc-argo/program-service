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

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.val;
import net.bytebuddy.utility.RandomString;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.InviteUserRequest;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc;
import org.icgc.argo.program_service.proto.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.proto.MembershipType.ASSOCIATE;
import static org.icgc.argo.program_service.UtilsTest.int32Value;
import static org.icgc.argo.program_service.UtilsTest.membershipTypeValue;
import static org.icgc.argo.program_service.UtilsTest.stringValue;
import static org.icgc.argo.program_service.UtilsTest.userRoleValue;

class ProgramServiceImplClientIT {
  private ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
  private ProgramServiceGrpc.ProgramServiceBlockingStub blockingStub = ProgramServiceGrpc.newBlockingStub(channel);

  @Test
  void createAndListPrograms() {
    val program = Program.newBuilder()
        .setShortName(stringValue(RandomString.make(10)))
        .setMembershipType(membershipTypeValue(ASSOCIATE))
        .setWebsite(stringValue(""))
        .setInstitutions(stringValue("oicr"))
        .setRegions(stringValue(""))
        .setName(stringValue(RandomString.make(15)))
        .setCommitmentDonors(int32Value(234))
        .setCountries(stringValue("canada"))
        .setSubmittedDonors(int32Value(244))
        .setGenomicDonors(int32Value(333))
        .setDescription(stringValue("nothing"));

    val createProgramRequest = CreateProgramRequest.newBuilder().setProgram(program).build();

    val response = blockingStub.createProgram(createProgramRequest);
    assertThat(response.getId().getValue()).isNotEmpty();
  }

  @Test
  void joinAndLeaveProgram() {
    val program = Program.newBuilder()
        .setShortName(stringValue(RandomString.make(10)))
        .setMembershipType(membershipTypeValue(ASSOCIATE))
        .setWebsite(stringValue(""))
        .setInstitutions(stringValue("oicr"))
        .setRegions(stringValue(""))
        .setName(stringValue(RandomString.make(15)))
        .setCommitmentDonors(int32Value(234))
        .setCountries(stringValue("canada"))
        .setSubmittedDonors(int32Value(244))
        .setGenomicDonors(int32Value(333))
        .setDescription(stringValue("nothing"));

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
    assertThat(inviteUserResponse.getInviteId().getValue()).isNotEmpty();
  }


}
