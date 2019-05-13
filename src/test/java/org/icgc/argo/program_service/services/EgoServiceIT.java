package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
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

  ProgramEntity programEntity;

  @BeforeAll
  void setUp() {
    val p = programService.getProgram("TestProgram");
    if (p.isPresent()) {
      this.programEntity = p.get();
      return;
    }

    val program = Program.newBuilder().setName("TestProgram").setShortName("TestShortName").build();
    this.programEntity = programService.createProgram(program);
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
  }
}