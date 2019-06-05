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
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.services.EgoService.EgoCollection;
import org.icgc.argo.program_service.services.EgoService.Permission;
import org.icgc.argo.program_service.services.EgoService.Policy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.MembershipType.ASSOCIATE;
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
  EgoService egoService;

  @Autowired
  ProgramService programService;

  @Autowired
  AppProperties appProperties;

  @Autowired
  CommonConverter commonConverter;

  ProgramEntity programEntity;

  private static final String ADMIN_USER_EMAIL = "lexishuhanli@gmail.com";
  private static final String COLLABORATOR_USER_EMAIL = "TestPS@dummy.com";

  @BeforeAll
  void setUp() {
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

    // Policies are created
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {}).isPresent()).isTrue();
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAMDATA-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {}).isPresent()).isTrue();

    val bannedGroup = egoService.getGroup("PROGRAM-TestShortName-BANNED");
    val curatorGroup = egoService.getGroup("PROGRAM-TestShortName-CURATOR");
    val collaboratorGroup = egoService.getGroup("PROGRAM-TestShortName-COLLABORATOR");
    val submitterGroup = egoService.getGroup("PROGRAM-TestShortName-SUBMITTER");
    val adminGroup = egoService.getGroup("PROGRAM-TestShortName-ADMIN");

    // Groups are created
    assertThat(bannedGroup.isPresent()).isTrue();
    assertThat(curatorGroup.isPresent()).isTrue();
    assertThat(collaboratorGroup.isPresent()).isTrue();
    assertThat(submitterGroup.isPresent()).isTrue();
    assertThat(adminGroup.isPresent()).isTrue();

    // Groups have permissions
    val bannedPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), bannedGroup.get().getId()), new ParameterizedTypeReference<EgoCollection<Permission>>() {}).toArray(
        Permission[]::new);
    assertThat(bannedPermissions.length).isEqualTo(2);
    assertThat(bannedPermissions).allMatch(permission -> permission.getAccessLevel().equals("DENY"));

    val curatorPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), curatorGroup.get().getId()), new ParameterizedTypeReference<EgoCollection<Permission>>() {}).toArray(
        Permission[]::new);
    assertThat(curatorPermissions.length).isEqualTo(2);
    assertThat(curatorPermissions).allMatch(permission -> permission.getAccessLevel().equals("WRITE"));

    val collaboratorPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), collaboratorGroup.get().getId()), new ParameterizedTypeReference<EgoCollection<Permission>>() {}).toArray(
        Permission[]::new);
    assertThat(collaboratorPermissions.length).isEqualTo(2);
    assertThat(collaboratorPermissions).allMatch(permission -> permission.getAccessLevel().equals("READ"));

    val submitterPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), submitterGroup.get().getId()), new ParameterizedTypeReference<EgoCollection<Permission>>() {}).toArray(
        Permission[]::new);
    assertThat(submitterPermissions.length).isEqualTo(2);
    assertThat(submitterPermissions).anyMatch(permission -> permission.getAccessLevel().equals("READ"));
    assertThat(submitterPermissions).anyMatch(permission -> permission.getAccessLevel().equals("WRITE"));

    val adminPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), adminGroup.get().getId()), new ParameterizedTypeReference<EgoCollection<Permission>>() {}).toArray(
        Permission[]::new);
    assertThat(adminPermissions.length).isEqualTo(2);
    assertThat(adminPermissions).allMatch(permission -> permission.getAccessLevel().equals("WRITE"));
  }

  @Test
  void egoServiceInitialization() {
    assertThat(ReflectionTestUtils.getField(egoService, "restTemplate")).isNotNull();
    assertThat(ReflectionTestUtils.getField(egoService, "egoPublicKey")).isNotNull();
  }

  @Test
  void getUser() {
    val egoUser1 = egoService.getUser("d8660091@gmail.com");
    val egoUser2 = egoService.getUser(ADMIN_USER_EMAIL);
    val egoUser3 = egoService.getUser(COLLABORATOR_USER_EMAIL);
    assertThat(egoUser1.isPresent()).isTrue();
    assertThat(egoUser2.isPresent()).isTrue();
    assertThat(egoUser3.isPresent()).isTrue();
  }

  @Test
  void joinAndLeaveProgram() {
    val result = egoService.joinProgram("d8660091@gmail.com", programEntity, UserRole.ADMIN);
    assertThat(result).isTrue();

    val groupId = egoService.getGroup("PROGRAM-TestShortName-ADMIN").get().getId();

    val user = egoService.getObject(String.format("%s/groups/%s/users?query=%s", appProperties.getEgoUrl(), groupId, "d8660091@gmail.com"), new ParameterizedTypeReference<EgoCollection<EgoService.EgoUser>>() {});
    assertThat(user.isPresent()).isTrue();

    egoService.leaveProgram("d8660091@gmail.com", programEntity.getId());
    assertThat(egoService.getObject(String.format("%s/groups/%s/users?query=%s", appProperties.getEgoUrl(), groupId, "d8660091@gmail.com"), new ParameterizedTypeReference<EgoCollection<EgoService.EgoUser>>() {}).isPresent()).isFalse();
  }

  @Test
  void listUser(){
    List<String> expectedUsers = new ArrayList();
    expectedUsers.add(ADMIN_USER_EMAIL);
    expectedUsers.add(COLLABORATOR_USER_EMAIL);

    val adminGroupId = egoService.getGroup("PROGRAM-TestShortName-ADMIN").get().getId();
    val collaboratorGroupId = egoService.getGroup("PROGRAM-TestShortName-COLLABORATOR").get().getId();

    val adminJoin = egoService.joinProgram(ADMIN_USER_EMAIL, programEntity, UserRole.ADMIN);
    assertThat(adminJoin).as("Can add ADMIN user to TestProgram.").isTrue();

    val collaboratorJoin = egoService.joinProgram(COLLABORATOR_USER_EMAIL, programEntity, UserRole.COLLABORATOR);
    assertThat(collaboratorJoin).as("Can add COLLABORATOR user to TestProgram.").isTrue();

    val users = egoService.getUserByGroup(programEntity.getId());
    users.forEach( user ->
            assertTrue(ifUserExists(commonConverter.unboxStringValue(user.getEmail()), expectedUsers)));

    assertThat(egoService.leaveProgram(ADMIN_USER_EMAIL, programEntity.getId()))
            .as("ADMIN user is removed from TestProgram.").isTrue();
    assertThat(egoService.leaveProgram(COLLABORATOR_USER_EMAIL, programEntity.getId()))
            .as("COLLABORATOR user is removed from TestProgram.").isTrue();

    assertThat(egoService.getObject(
                String.format("%s/groups/%s/users?query=%s", appProperties.getEgoUrl(), adminGroupId, ADMIN_USER_EMAIL),
                new ParameterizedTypeReference<EgoCollection<EgoService.EgoUser>>() {})
        .isPresent()).isFalse();

    assertThat(egoService.getObject(
            String.format("%s/groups/%s/users?query=%s", appProperties.getEgoUrl(), collaboratorGroupId, COLLABORATOR_USER_EMAIL),
            new ParameterizedTypeReference<EgoCollection<EgoService.EgoUser>>() {})
        .isPresent()).isFalse();
  }

  private boolean ifUserExists(String email, List<String> userList){
    return userList.contains(email);
  }

  @AfterAll
  void cleanUp() {
    programService.removeProgram(this.programEntity);

    // Groups are removed
    assertThat(egoService.getGroup("PROGRAM-TestShortName-BANNED").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-CURATOR").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-COLLABORATOR").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-SUBMITTER").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-ADMIN").isPresent()).isFalse();

    // Policies are removed
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {}).isPresent()).isFalse();
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAMDATA-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {}).isPresent()).isFalse();
  }
}
