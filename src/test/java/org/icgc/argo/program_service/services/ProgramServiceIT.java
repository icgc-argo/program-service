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
import lombok.extern.slf4j.Slf4j;
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
import static org.assertj.core.api.Assertions.setMaxElementsForPrinting;
import static org.icgc.argo.program_service.UtilsTest.*;
import static org.icgc.argo.program_service.proto.MembershipType.ASSOCIATE;

@Slf4j
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
  MailService mailService;

  @Autowired
  JoinProgramInviteRepository inviteRepository;

  private static final String ADMIN_USER_EMAIL = "lexishuhanli@gmail.com";
  private static final String name="TEST-PROGRAM-X-CA";
  @BeforeAll
  void setUp() {
    egoService = new EgoService(repository, converter, client, mailService, inviteRepository);

    try {
      egoService.cleanUpProgram(name);
    } catch(Throwable t) {
      log.error(t.getMessage());
    }
  }


  @Test
  public void test_setupProgram() {
    egoService.setUpProgram(name, List.of(ADMIN_USER_EMAIL));

    // Policies are created
    assertThat(client.getPolicyByName("PROGRAM-" + name).isPresent()).isTrue();
    assertThat(client.getPolicyByName("PROGRAMDATA-" + name).isPresent()).isTrue();

    for(UserRole role: UserRole.values()) {
      if (role == UserRole.UNRECOGNIZED) { continue; }
      verifyRole(role, name);
    }

    test_removeProgram(name);
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


  public void test_removeProgram(String name) {
    egoService.cleanUpProgram(name);
    Throwable throwable=null;
    try {
      egoService.getProgramEgoGroup(name, UserRole.ADMIN);
    } catch(Throwable t) {
      throwable = t;
    }
    assertThat(throwable).isNotNull();

    // Groups are removed
    assertThat(client.getGroupByName("PROGRAM-" + name + "-BANNED").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-" + name + "-CURATOR").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-" + name + "-COLLABORATOR").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-" + name + "-SUBMITTER").isPresent()).isFalse();
    assertThat(client.getGroupByName("PROGRAM-" + name + "-ADMIN").isPresent()).isFalse();

    // Policies are removed
    assertThat(client.getPolicyByName("PROGRAM-" + name).isPresent()).isFalse();
    assertThat(client.getPolicyByName("PROGRAMDATA-" + name).isPresent()).isFalse();
  }
}
