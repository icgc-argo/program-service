/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.services;

import static java.lang.String.format;
import static org.icgc.argo.program_service.services.ego.EgoService.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoPermission;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

@Slf4j
@SpringBootTest
@ActiveProfiles({"test", "default"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgramServiceIT {

  static Network egoNetwork = Network.newNetwork();

  static PostgreSQLContainer<?> egoPostgres =
      new PostgreSQLContainer<>("postgres:18")
          .withNetwork(egoNetwork)
          .withNetworkAliases("ego-postgres")
          .withDatabaseName("ego")
          .withUsername("postgres")
          .withPassword("password");

  @SuppressWarnings("resource")
  static GenericContainer<?> ego =
      new GenericContainer<>("overture/ego:5.4.0")
          .withNetwork(egoNetwork)
          .withExposedPorts(8080)
          .withEnv("SERVER_PORT", "8080")
          .withEnv(
              "SPRING_DATASOURCE_URL",
              "jdbc:postgresql://ego-postgres:5432/ego?stringtype=unspecified")
          .withEnv("SPRING_DATASOURCE_USERNAME", "postgres")
          .withEnv("SPRING_DATASOURCE_PASSWORD", "password")
          .withEnv("SPRING_FLYWAY_ENABLED", "true")
          .withEnv("SPRING_FLYWAY_LOCATIONS", "classpath:flyway/sql,classpath:db/migration")
          .withEnv("SPRING_PROFILES", "demo,auth")
          .dependsOn(egoPostgres)
          .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200));

  static {
    Startables.deepStart(egoPostgres, ego).join();
  }

  @DynamicPropertySource
  static void egoProperties(DynamicPropertyRegistry registry) {
    registry.add("app.egoUrl", () -> "http://localhost:" + ego.getMappedPort(8080));
  }

  @Autowired EgoRESTClient client;

  @Autowired EgoService egoService;

  private static final String name = "TEST-X-CA";

  @BeforeAll
  void setUp() {
    try {
      egoService.cleanUpProgram(name);
    } catch (Throwable throwable) {
      log.warn("cleanUpProgram on setUp threw: {}", throwable.getMessage());
    }
  }

  @Test
  public void test_setupProgram() {
    egoService.setUpProgram(name);

    // Policies are created
    assertTrue(client.getPolicyByName("PROGRAM-" + name).isPresent());
    assertTrue(client.getPolicyByName("PROGRAMDATA-" + name).isPresent());

    for (UserRole role : roles()) {
      verifyRole(role, name);
    }

    test_removeProgram(name);
  }

  void verifyRole(UserRole role, String shortName) {
    log.info("Verifying role {}", role);
    val groupName = format("PROGRAM-%s-%s", shortName, role.toString());
    val group = client.getGroupByName(groupName);
    assertTrue(group.isPresent());

    val permissions = client.getGroupPermissions(group.get().getId());
    assertEquals(2, permissions.length);

    assertTrue(
        Arrays.stream(permissions)
            .anyMatch(
                permission ->
                    permissionsMatch(permission, "PROGRAM-" + shortName, getProgramMask(role))));

    assertTrue(
        Arrays.stream(permissions)
            .anyMatch(
                permission ->
                    permissionsMatch(permission, "PROGRAMDATA-" + shortName, getDataMask(role))));
  }

  private boolean permissionsMatch(
      EgoPermission permission, String policyName, String accessLevel) {
    return permission.getAccessLevel().equals(accessLevel)
        && permission.getPolicy().getName().equals(policyName);
  }

  public void test_removeProgram(String name) {
    egoService.cleanUpProgram(name);
    Throwable throwable = null;
    try {
      egoService.getProgramEgoGroup(name, UserRole.ADMIN);
    } catch (Throwable t) {
      throwable = t;
    }
    assertNotNull(throwable);

    // Groups are removed
    assertFalse(client.getGroupByName("PROGRAM-" + name + "-BANNED").isPresent());
    assertFalse(client.getGroupByName("PROGRAM-" + name + "-CURATOR").isPresent());
    assertFalse(client.getGroupByName("PROGRAM-" + name + "-COLLABORATOR").isPresent());
    assertFalse(client.getGroupByName("PROGRAM-" + name + "-SUBMITTER").isPresent());
    assertFalse(client.getGroupByName("PROGRAM-" + name + "-ADMIN").isPresent());

    // Policies are removed
    assertFalse(client.getPolicyByName("PROGRAM-" + name).isPresent());
    assertFalse(client.getPolicyByName("PROGRAMDATA-" + name).isPresent());
  }
}
