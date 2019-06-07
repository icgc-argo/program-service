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

package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.proto.MembershipType.ASSOCIATE;
import static org.icgc.argo.program_service.UtilsTest.int32Value;
import static org.icgc.argo.program_service.UtilsTest.membershipTypeValue;
import static org.icgc.argo.program_service.UtilsTest.stringValue;

@SpringBootTest
@ActiveProfiles({ "test", "default" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/program_db",
        "spring.datasource.driverClassName=org.postgresql.Driver",
})
class EgoServiceIT {
  @Autowired
  EgoRESTClient client;

  EgoService egoService;

  @Autowired
  ProgramEgoGroupRepository repository;
  @Autowired
  ProgramConverter converter;
  @Autowired
  ProgramService programService;

  @Autowired
  AppProperties appProperties;

  @Autowired
  CommonConverter commonConverter;

  ProgramEntity programEntity;

  private static final String TEST_EMAIL = "d8660091@gmail.com";
  private static final String ADMIN_USER_EMAIL = "lexishuhanli@gmail.com";
  private static final String COLLABORATOR_USER_EMAIL = "TestPS@dummy.com";

  @BeforeAll
  void setUp() {
    //val retryTemplate = new RetryTemplate();
    //client = new EgoRESTClient(retryTemplate, retryTemplate, appProperties);
    egoService = new EgoService(repository, converter, client);

    val p = programService.getProgram("TestProgram");
    if (p.isPresent()) {
      this.programEntity = p.get();
      return;
    }

    val program = Program.newBuilder()
        .setName(stringValue("TestProgram"))
        .setShortName(stringValue("TestShortName"))
        .setCommitmentDonors(int32Value(0))
        .setGenomicDonors(int32Value(0))
        .setSubmittedDonors(int32Value(0))
        .setMembershipType(membershipTypeValue(ASSOCIATE))
        .setWebsite(stringValue("https://example.com"))
        .setCountries(stringValue("Canada"))
        .setInstitutions(stringValue("oicr"))
        .setRegions(stringValue("toronto"))
        .setDescription(stringValue(""))
        .build();

    this.programEntity = programService.createProgram(program, List.of());
  }


  @Test
  void getUser() {
    val egoUser1 = client.getUser(TEST_EMAIL);
    val egoUser2 = client.getUser(ADMIN_USER_EMAIL);
    val egoUser3 = client.getUser(COLLABORATOR_USER_EMAIL);
    assertThat(egoUser1.isPresent()).isTrue();
    assertThat(egoUser2.isPresent()).isTrue();
    assertThat(egoUser3.isPresent()).isTrue();
  }

  @Test
  void joinAndLeaveProgram() {
    val result = egoService.joinProgram(TEST_EMAIL, programEntity, UserRole.ADMIN);
    assertThat(result).isTrue();

    val user = client.getUser(TEST_EMAIL);
    assertThat(user.isPresent()).isTrue();

    val groupId = client.getGroup("PROGRAM-TestShortName-ADMIN").get().getId();
    assertThat(client.isMember(groupId, TEST_EMAIL)).isTrue();

    egoService.leaveProgram(TEST_EMAIL, programEntity.getId());
    assertThat(client.isMember(groupId, TEST_EMAIL)).isFalse();
  }

  @Test
  void listUser(){
    List<String> expectedUsers = new ArrayList();
    expectedUsers.add(ADMIN_USER_EMAIL);
    expectedUsers.add(COLLABORATOR_USER_EMAIL);

    val adminGroupId = client.getGroup("PROGRAM-TestShortName-ADMIN").get().getId();
    val collaboratorGroupId = client.getGroup("PROGRAM-TestShortName-COLLABORATOR").get().getId();

    val adminJoin = egoService.joinProgram(ADMIN_USER_EMAIL, programEntity, UserRole.ADMIN);
    assertThat(adminJoin).as("Can add ADMIN user to TestProgram.").isTrue();

    val collaboratorJoin = egoService.joinProgram(COLLABORATOR_USER_EMAIL, programEntity, UserRole.COLLABORATOR);
    assertThat(collaboratorJoin).as("Can add COLLABORATOR user to TestProgram.").isTrue();

    val users = egoService.getUsersInGroup(programEntity.getId());
    users.forEach( user ->
            assertTrue(ifUserExists(commonConverter.unboxStringValue(user.getEmail()), expectedUsers)));

    assertThat(egoService.leaveProgram(ADMIN_USER_EMAIL, programEntity.getId()))
            .as("ADMIN user is removed from TestProgram.").isTrue();
    assertThat(egoService.leaveProgram(COLLABORATOR_USER_EMAIL, programEntity.getId()))
            .as("COLLABORATOR user is removed from TestProgram.").isTrue();

    assertThat(client.isMember(adminGroupId, ADMIN_USER_EMAIL)).isFalse();
    assertThat(client.isMember(collaboratorGroupId, COLLABORATOR_USER_EMAIL)).isFalse();
  }

  private boolean ifUserExists(String email, List<String> userList){
    return userList.contains(email);
  }

  @AfterAll
  void cleanUp() {
    programService.removeProgram(this.programEntity);
  }
}
