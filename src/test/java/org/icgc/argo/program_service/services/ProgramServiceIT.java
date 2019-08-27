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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

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
  JoinProgramInviteRepository inviteRepository;

  @Autowired
  ProgramService programService;

  @Autowired
  ProgramRepository programRepository;

  private static final String name="TEST-X-CA";

  @BeforeAll
  void setUp() {
    System.err.printf("Setting up...\n");
    egoService = new EgoService(repository, converter, client, inviteRepository);

    try {
      egoService.cleanUpProgram(name);
    } catch(Throwable t) {
      System.err.printf("Caught throwable with message: %s", t.getMessage());
    }
  }

  @Test
  public void test_setupProgram() {
    egoService.setUpProgram(name);

    // Policies are created
    assertTrue(client.getPolicyByName("PROGRAM-" + name).isPresent());
    assertTrue(client.getPolicyByName("PROGRAMDATA-" + name).isPresent());

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
    assertTrue(group.isPresent());

    val permissions = client.getGroupPermissions(group.get().getId());
    assertEquals(2, permissions.length);
    assertTrue(Arrays.asList(permissions).stream().
        anyMatch(permission -> permission.getAccessLevel().equals(EgoService.getProgramMask(role))));
    assertTrue(Arrays.asList(permissions).stream().
      anyMatch(permission -> permission.getAccessLevel().equals(EgoService.getDataMask(role))));
  }


  public void test_removeProgram(String name) {
    egoService.cleanUpProgram(name);
    Throwable throwable=null;
    try {
      egoService.getProgramEgoGroup(name, UserRole.ADMIN);
    } catch(Throwable t) {
      throwable = t;
    }
    assertNotNull(throwable);

    // Groups are removed
    assertFalse( client.getGroupByName("PROGRAM-" + name + "-BANNED").isPresent());
    assertFalse( client.getGroupByName("PROGRAM-" + name + "-CURATOR").isPresent());
    assertFalse( client.getGroupByName("PROGRAM-" + name + "-COLLABORATOR").isPresent());
    assertFalse( client.getGroupByName("PROGRAM-" + name + "-SUBMITTER").isPresent());
    assertFalse( client.getGroupByName("PROGRAM-" + name + "-ADMIN").isPresent());

    // Policies are removed
    assertFalse( client.getPolicyByName("PROGRAM-" + name).isPresent());
    assertFalse( client.getPolicyByName("PROGRAMDATA-" + name).isPresent());
  }

}
