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

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertTrue;
import static org.icgc.argo.program_service.UtilsTest.*;
import static org.icgc.argo.program_service.proto.MembershipType.ASSOCIATE;
import static org.junit.Assert.*;

@SpringBootTest
@ActiveProfiles({ "test", "default" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:postgresql://localhost:5432/program_db",
  "spring.datasource.driverClassName=org.postgresql.Driver",
})
@ComponentScan(lazyInit = true)
class EgoServiceIT {

  @Autowired
  EgoRESTClient client;

  @Autowired
  ProgramEgoGroupRepository repository;

  @Autowired
  ProgramConverter converter;

  @Autowired
  JoinProgramInviteRepository inviteRepository;

  @Autowired
  ProgramService programService;

  @Autowired
  AppProperties appProperties;

  @Autowired
  CommonConverter commonConverter;

  ProgramEntity programEntity;

  EgoService egoService;

  private static final String TEST_EMAIL = "d8660091@gmail.com";
  private static final String ADMIN_USER_EMAIL = "lexishuhanli@gmail.com";
  private static final String COLLABORATOR_USER_EMAIL = "TestPS@dummy.com";
  private static final String UPDATE_USER_TEST_EMAIL = "Test_update_user_PS@dummy.com";
  private static final String PROGRAM_NAME = "PROGRAM-DK";

  @BeforeAll
  void setUp() {
    egoService = new EgoService(repository, converter, client, inviteRepository);
    setUpUser(TEST_EMAIL);
    setUpUser(ADMIN_USER_EMAIL);
    setUpUser(COLLABORATOR_USER_EMAIL);
    setUpUser(UPDATE_USER_TEST_EMAIL);
    programEntity = setupProgram();
  }

  private ProgramEntity setupProgram() {
    try {
      val p = programService.getProgram(PROGRAM_NAME);
      System.err.println("Test program is already set up...");
      return p;
    } catch (Exception e) {
      if (!isNotFoundException(e)) {
        System.err.println("Caught exception during setup" + e.getMessage());
        throw e;
      }
    }

    val program = Program.newBuilder()
      .setName(stringValue("Test Program Number One"))
      .setShortName(stringValue(PROGRAM_NAME))
      .setCommitmentDonors(int32Value(0))
      .setGenomicDonors(int32Value(0))
      .setSubmittedDonors(int32Value(0))
      .setMembershipType(membershipTypeValue(ASSOCIATE))
      .setWebsite(stringValue("https://example.com"))
      .setDescription(stringValue(""))
      .addCountries("Canada")
      .addRegions("North America")
      .addInstitutions("Ontario Institute for Cancer Research")
      .addCancerTypes("Blood cancer")
      .addPrimarySites("Blood")
      .build();

    egoService.setUpProgram(PROGRAM_NAME);
    return programService.createProgram(program);
  }

  private boolean isNotFoundException(Exception e) {
    if (e instanceof StatusRuntimeException) {
      val ex = (StatusRuntimeException) e;
      
      val status = ex.getStatus();
      if ( status.getCode() == Status.NOT_FOUND.getCode()) {
        return true;
      }
    }
    return false;
  }

  public EgoUser setUpUser(String email) {
    val user = egoService.getEgoClient().getUser(email);
    if (user.isPresent()) {
      return user.get();
    }
    return egoService.getEgoClient().createEgoUser(email, "", "");
  }

  @Test
  public void updateUser() {
    val user = egoService.getEgoClient().getUser(UPDATE_USER_TEST_EMAIL);
    assertTrue(user.isPresent());

    // add user to COLLABORATOR group
    assertTrue(egoService.joinProgram(UPDATE_USER_TEST_EMAIL, programEntity.getShortName(), UserRole.COLLABORATOR));

    val groupBefore = egoService.getEgoClient().getGroupsByUserId(user.get().getId()).
      collect(Collectors.toUnmodifiableList());
    assertEquals(groupBefore.size(), 1);
    groupBefore.forEach(group -> assertEquals(group.getName(), "PROGRAM-"+PROGRAM_NAME +"-COLLABORATOR"));

    // expected group is ADMIN group
    val shortname = PROGRAM_NAME;
    egoService.updateUserRole(user.get().getEmail(), shortname, UserRole.ADMIN);
    val adminGroupName = "PROGRAM-"+ PROGRAM_NAME +"-ADMIN";
    val adminGroupId = egoService.getEgoClient().getGroupByName(adminGroupName).get().getId();

    // verify if user role is updated to ADMIN
    val groupAfter = egoService.getEgoClient().getGroupsByUserId(user.get().getId())
      .collect(Collectors.toUnmodifiableList());
    assertEquals(groupAfter.size(), 1);
    groupAfter.forEach(group -> assertEquals(group.getId(), adminGroupId));

    // remove test user from admin group
    assertTrue(egoService.leaveProgram(user.get().getEmail(), programEntity.getShortName()));

    //verify if the user is removed from a admin group
    val groupsLeft = egoService.getEgoClient().getGroupsByUserId(user.get().getId())
      .collect(Collectors.toUnmodifiableList());
    assertEquals(groupsLeft.size(),0);
  }

  @Test
  void egoServiceInitialization() {
    assertNotNull(ReflectionTestUtils.getField(egoService, "egoPublicKey"));
  }

  @Test
  void getUser() {
    val egoUser1 = client.getUser(TEST_EMAIL);
    val egoUser2 = client.getUser(ADMIN_USER_EMAIL);
    val egoUser3 = client.getUser(COLLABORATOR_USER_EMAIL);
    assertTrue(egoUser1.isPresent());
    assertTrue(egoUser2.isPresent());
    assertTrue(egoUser3.isPresent());
  }

  @Test
  void joinAndLeaveProgram() {
    val result = egoService.joinProgram(COLLABORATOR_USER_EMAIL, programEntity.getShortName(), UserRole.ADMIN);
    assertTrue(result);

    val user = client.getUser(COLLABORATOR_USER_EMAIL);
    assertTrue(user.isPresent());

    val groupId = client.getGroupByName("PROGRAM-"+PROGRAM_NAME+"-ADMIN").get().getId();
    assertTrue(client.isMember(groupId, COLLABORATOR_USER_EMAIL));

    egoService.leaveProgram(COLLABORATOR_USER_EMAIL, programEntity.getShortName());
    assertFalse(client.isMember(groupId, COLLABORATOR_USER_EMAIL));
  }

  @Test
  void listUser() {
    List<String> expectedUsers = new ArrayList();
    expectedUsers.add(ADMIN_USER_EMAIL);
    expectedUsers.add(COLLABORATOR_USER_EMAIL);
    expectedUsers.add(TEST_EMAIL);

    val adminGroupId = client.getGroupByName("PROGRAM-"+PROGRAM_NAME+"-ADMIN").get().getId();
    val collaboratorGroupId = client.getGroupByName("PROGRAM-"+PROGRAM_NAME+"-COLLABORATOR").get().getId();

    val adminJoin = egoService.joinProgram(ADMIN_USER_EMAIL, programEntity.getShortName(), UserRole.ADMIN);
    assertTrue("Can add ADMIN user to Program.", adminJoin);

    val collaboratorJoin = egoService.joinProgram(COLLABORATOR_USER_EMAIL, programEntity.getShortName(), UserRole.COLLABORATOR);
    assertTrue("Can add COLLABORATOR user to Program.", collaboratorJoin);

    val users = egoService.getUsersInProgram(programEntity.getShortName());
    users.forEach(user ->
      assertTrue(ifUserExists(user.getEmail().getValue(), expectedUsers)));

    assertTrue("ADMIN user is removed from Program.",
      egoService.leaveProgram(ADMIN_USER_EMAIL, programEntity.getShortName()));
    assertTrue("COLLABORATOR user is removed from Program.",
      egoService.leaveProgram(COLLABORATOR_USER_EMAIL, programEntity.getShortName()));

    assertFalse(client.isMember(adminGroupId, ADMIN_USER_EMAIL));
    assertFalse(client.isMember(collaboratorGroupId, COLLABORATOR_USER_EMAIL));
  }

  private boolean ifUserExists(String email, List<String> userList) {
    return userList.contains(email);
  }

  @AfterAll
  void cleanUp() {
    val ego = egoService.getEgoClient();
    try {
      egoService.cleanUpProgram(PROGRAM_NAME);
      programService.removeProgram(PROGRAM_NAME);
    } catch (Throwable throwable) {
      System.err.println("Remove program threw" + throwable.getMessage());
    }
    // Groups are removed
    for (val groupName : List.of("PROGRAM-"+PROGRAM_NAME+"-BANNED", "PROGRAM-"+PROGRAM_NAME+"-CURATOR",
      "PROGRAM-"+PROGRAM_NAME+"-COLLABORATOR", "PROGRAM-"+PROGRAM_NAME+"-SUBMITTER", "PROGRAM-"+PROGRAM_NAME+"-ADMIN")) {
      assertTrue(ego.getGroupByName(groupName).isEmpty());
    }
    // Policies are removed
    assertTrue(ego.getPolicyByName("PROGRAM-" + programEntity.getShortName()).isEmpty());
    assertTrue(ego.getPolicyByName("PROGRAMDATA-" + programEntity.getShortName()).isEmpty());
  }

}
