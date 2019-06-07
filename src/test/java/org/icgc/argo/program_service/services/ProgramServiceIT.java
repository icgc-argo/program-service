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
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import java.util.List;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.UtilsTest.*;
import static org.icgc.argo.program_service.proto.MembershipType.ASSOCIATE;

@SpringBootTest
@ActiveProfiles({ "test", "default" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/program_db",
        "spring.datasource.driverClassName=org.postgresql.Driver",
})
class ProgramServiceIT {
  EgoService egoService;

  @Autowired
  EgoRESTClient client;
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

  private static final String ADMIN_USER_EMAIL = "lexishuhanli@gmail.com";
  private static final String COLLABORATOR_USER_EMAIL = "TestPS@dummy.com";

  @BeforeAll
  void setUp() {
    //val retryTemplate = new RetryTemplate();
    //client = new EgoRESTClient(retryTemplate, retryTemplate, appProperties);
    egoService = new EgoService(repository, converter, client);

    val p = programService.getProgram("TestProgram");
    if (p.isPresent()) {
      test_removeProgram(p.get());
    }
  }

  @Test
  public void test_createProgram() {
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

    val programEntity = programService.createProgram(program, List.of());

    // Policies are created
    assertThat(client.getPolicyByName("PROGRAM-" + programEntity.getShortName()).isPresent()).isTrue();
    assertThat(client.getPolicyByName("PROGRAMDATA-" + programEntity.getShortName()).isPresent()).isTrue();

    for(UserRole role: UserRole.values()) {
      if (role == UserRole.UNRECOGNIZED) { continue; }
      verifyRole(role, programEntity.getShortName());
    }

    test_removeProgram(programEntity);
  }

  void verifyRole(UserRole role, String shortName) {
    System.err.println("verifying role" + role);
    val name = format("PROGRAM-%s-%s", shortName, role.toString());
    val group = client.getGroupByName(name);
    assertThat(group.isPresent()).isTrue();

    val permissions = client.getGroupPermissions(group.get().getId());
    assertThat(permissions.length).isEqualTo(2);
    assertThat(permissions).anyMatch(permission -> permission.getAccessLevel().equals(egoService.getProgramMask(role)));
    assertThat(permissions).anyMatch(permission -> permission.getAccessLevel().equals(egoService.getDataMask(role)));
  }


  public void test_removeProgram(ProgramEntity programEntity) {
    programService.removeProgram(programEntity);

    // Groups are removed
    assertThat(client.getGroupByName("PROGRAM-TestShortName-BANNED").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-TestShortName-CURATOR").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-TestShortName-COLLABORATOR").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-TestShortName-SUBMITTER").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-TestShortName-ADMIN").isPresent()).isFalse();

    // Policies are removed
    assertThat(client.getPolicyByName("PROGRAM-" + programEntity.getShortName()).isPresent()).isFalse();
    assertThat(client.getPolicyByName("PROGRAMDATA-" + programEntity.getShortName()).isPresent()).isFalse();
  }
}
