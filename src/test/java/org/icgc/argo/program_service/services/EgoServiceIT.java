package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.properties.AppProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.MembershipType.ASSOCIATE;
import static org.icgc.argo.program_service.UtilsTest.*;

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

  private ProgramEntity programEntity;

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
    this.programEntity = programService.createProgram(program);
    assertThat(this.programEntity).isNotNull();

    // Policies are created
    assertThat(egoService.getPolicy("PROGRAM-" + programEntity.getShortName()).isPresent()).isTrue();
    assertThat(egoService.getPolicy("PROGRAM-DATA-" + programEntity.getShortName()).isPresent()).isTrue();

    val bannedGroup = egoService.getGroup("PROGRAM-TestShortName-BANNED").get();
    val curatorGroup = egoService.getGroup("PROGRAM-TestShortName-CURATOR").get();
    val collaboratorGroup = egoService.getGroup("PROGRAM-TestShortName-COLLABORATOR").get();
    val submitterGroup = egoService.getGroup("PROGRAM-TestShortName-SUBMITTER").get();
    val adminGroup = egoService.getGroup("PROGRAM-TestShortName-ADMIN").get();

    // Groups are created
    assertThat(bannedGroup).isNotNull();
    assertThat(curatorGroup).isNotNull();
    assertThat(collaboratorGroup).isNotNull();
    assertThat(submitterGroup).isNotNull();
    assertThat(adminGroup).isNotNull();

    // Groups have permissions
    assertAllPermissions(bannedGroup, "DENY");
    assertAllPermissions(curatorGroup, "WRITE");
    assertAllPermissions(collaboratorGroup, "READ");

    val submitterPermissions = egoService.getGroupPermissions(submitterGroup.getId());
    assertThat(submitterPermissions.count()).isEqualTo(2);
    assertThat(submitterPermissions).anyMatch(permission -> permission.getAccessLevel().equals("READ"));
    assertThat(submitterPermissions).anyMatch(permission -> permission.getAccessLevel().equals("WRITE"));

    assertAllPermissions(adminGroup, "WRITE");
  }

  private void assertAllPermissions(EgoService.Group group, String mask) {
    val permissions = egoService.getGroupPermissions(group.getId());
    assertThat(permissions.count()).isEqualTo(2);
    assertThat(permissions).allMatch(permission -> permission.getAccessLevel().equals(mask));

  }

  @Test
  void egoServiceInitialization() {
    assertThat(ReflectionTestUtils.getField(egoService, "restTemplate")).isNotNull();
    assertThat(ReflectionTestUtils.getField(egoService, "egoPublicKey")).isNotNull();
  }

  @Test
  void getUser() {
    val egoUser = egoService.getUser("d8660091@gmail.com");

    assertThat(egoUser.isPresent()).isTrue();
  }

  @Test
  void joinAndLeaveProgram() {
    val result = egoService.joinProgram("d8660091@gmail.com", programEntity, UserRole.ADMIN);
    assertThat(result).isTrue();

    val groupId = egoService.getGroup("PROGRAM-TestShortName-ADMIN").get().getId();

    val user = egoService.getGroupUser(groupId, "d8660091@gmail.com");
    assertThat(user.isPresent()).isTrue();

    egoService.leaveProgram("d8660091@gmail.com", programEntity.getId());

    assertThat(egoService.getObject(
      String.format("/groups/%s/users?query=%s", groupId, "d8660091@gmail.com"),
      EgoService.User.class).isPresent()).isFalse();
  }

  @AfterAll
  void cleanUp() {
    try {
      programService.removeProgram(this.programEntity);
    } catch (Throwable t) {
      System.err.println("Caught exception:" + t.getMessage());
    }
    // Groups are removed
    assertThat(egoService.getGroup("PROGRAM-TestShortName-BANNED").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-CURATOR").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-COLLABORATOR").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-SUBMITTER").isPresent()).isFalse();
    assertThat(egoService.getGroup("PROGRAM-TestShortName-ADMIN").isPresent()).isFalse();

    // Policies are removed
    assertThat(egoService.getPolicy("PROGRAM-" + programEntity.getShortName()).isPresent()).isFalse();
    assertThat(egoService.getPolicy("PROGRAM-DATA-" + programEntity.getShortName()).isPresent()).isFalse();
  }
}