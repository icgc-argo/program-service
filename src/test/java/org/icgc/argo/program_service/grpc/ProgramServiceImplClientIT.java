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
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.utils.EntityGenerator;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.UtilsTest.*;
import static org.icgc.argo.program_service.proto.MembershipType.ASSOCIATE;

@SpringBootTest
@RunWith(SpringRunner.class)
class ProgramServiceImplClientIT {

  private ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build();
  private ProgramServiceGrpc.ProgramServiceBlockingStub blockingStub = ProgramServiceGrpc.newBlockingStub(channel);

  private final String NEW_USER_EMAIL = "hermionegranger@gmail.com";
  private final String PROGRAM_SHORT_NAME = "PROGRAM-CA";

  @Autowired
  private CommonConverter commonConverter;

  @Autowired
  private EgoClient egoClient;

  @Autowired
  private EntityGenerator entityGenerator;

  @Test
  void createAndListPrograms() {
    val program = Program.newBuilder()
      .setShortName(stringValue(randomProgramName()))
      .setMembershipType(membershipTypeValue(ASSOCIATE))
      .setWebsite(stringValue("http://site.org"))
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
    assertThat(response.getCreatedAt().toString()).isNotEmpty();
  }

  @Test
  void joinAndLeaveProgram() {
    val name = stringValue(randomProgramName());
    val program = Program.newBuilder()
      .setShortName(name)
      .setMembershipType(membershipTypeValue(ASSOCIATE))
      .setWebsite(stringValue("http://site.org"))
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

    val inviteUserRequest = InviteUserRequest.newBuilder()
      .setFirstName(stringValue("First"))
      .setLastName(stringValue("Last"))
      .setEmail(stringValue("user@example.com"))
      .setRole(userRoleValue(UserRole.ADMIN))
      .setProgramShortName(name)
      .build();
    val inviteUserResponse = blockingStub.inviteUser(inviteUserRequest);
    assertThat(inviteUserResponse.getInviteId().getValue()).isNotEmpty();
  }

  @Test
  public void invite_new_user_user_gets_added(){
    entityGenerator.setUpProgramEntity(PROGRAM_SHORT_NAME);
    assertThat(egoClient.getUser(NEW_USER_EMAIL).isPresent()).isFalse();

    val request = InviteUserRequest.newBuilder()
            .setEmail(commonConverter.boxString(NEW_USER_EMAIL))
            .setFirstName(commonConverter.boxString("Hermione"))
            .setLastName(commonConverter.boxString("Granger"))
            .setProgramShortName(commonConverter.boxString(PROGRAM_SHORT_NAME))
            .build();
    val response = blockingStub.inviteUser(request);

    assertThat(response.getInviteId()).isNotNull();
    assertThat(egoClient.getUser(NEW_USER_EMAIL).isPresent()).isTrue();
    val user = egoClient.getUser(NEW_USER_EMAIL).get();

    assertThat(egoClient.getUser(NEW_USER_EMAIL).get().getFirstName()).isEqualTo("Hermione");
    assertThat(egoClient.getUser(NEW_USER_EMAIL).get().getLastName()).isEqualTo("Granger");

    egoClient.deleteUserById(user.getId());
    assertThat(egoClient.getUser(NEW_USER_EMAIL).isPresent()).isFalse();
  }

  String randomProgramName() {
    return randomAlphabetic(7).toUpperCase() + "-CA";
  }
}
