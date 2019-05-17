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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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

  ProgramEntity programEntity;

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
        .build();
    this.programEntity = programService.createProgram(program);

    // Policies are created
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Policy>>() {}).isPresent()).isTrue();
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-DATA-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Policy>>() {}).isPresent()).isTrue();

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
    val bannedPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), bannedGroup.get().getId()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Permission>>() {}).toArray(EgoService.Permission[]::new);
    assertThat(bannedPermissions.length).isEqualTo(2);
    assertThat(bannedPermissions).allMatch(permission -> permission.getAccessLevel().equals("DENY"));

    val curatorPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), curatorGroup.get().getId()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Permission>>() {}).toArray(EgoService.Permission[]::new);
    assertThat(curatorPermissions.length).isEqualTo(2);
    assertThat(curatorPermissions).allMatch(permission -> permission.getAccessLevel().equals("WRITE"));

    val collaboratorPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), collaboratorGroup.get().getId()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Permission>>() {}).toArray(EgoService.Permission[]::new);
    assertThat(collaboratorPermissions.length).isEqualTo(2);
    assertThat(collaboratorPermissions).allMatch(permission -> permission.getAccessLevel().equals("READ"));

    val submitterPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), submitterGroup.get().getId()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Permission>>() {}).toArray(EgoService.Permission[]::new);
    assertThat(submitterPermissions.length).isEqualTo(2);
    assertThat(submitterPermissions).anyMatch(permission -> permission.getAccessLevel().equals("READ"));
    assertThat(submitterPermissions).anyMatch(permission -> permission.getAccessLevel().equals("WRITE"));

    val adminPermissions = egoService.getObjects(String.format("%s/groups/%s/permissions", appProperties.getEgoUrl(), adminGroup.get().getId()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Permission>>() {}).toArray(EgoService.Permission[]::new);
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
    val egoUser = egoService.getUser("d8660091@gmail.com");

    assertThat(egoUser.isPresent()).isTrue();
  }

  @Test
  void joinProgram() {
    val result = egoService.joinProgram("d8660091@gmail.com", programEntity, UserRole.ADMIN);

    assertThat(result).isTrue();
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
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Policy>>() {}).isPresent()).isFalse();
    assertThat(egoService.getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-DATA-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoService.EgoCollection<EgoService.Policy>>() {}).isPresent()).isFalse();
  }
}