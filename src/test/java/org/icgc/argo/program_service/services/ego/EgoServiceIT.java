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

package org.icgc.argo.program_service.services.ego;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static junit.framework.TestCase.*;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.time.Duration;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.services.ego.model.exceptions.EgoException;
import org.icgc.argo.program_service.utils.EntityGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("test")
public class EgoServiceIT {

  private EgoRESTClient client;

  @Autowired private RetryTemplate lenientRetryTemplate;

  @Autowired private RetryTemplate retryTemplate;

  @Autowired EntityGenerator entityGenerator;

  @Autowired ProgramConverter converter;

  @Autowired JoinProgramInviteRepository inviteRepository;

  private EgoService egoService;

  @Rule public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  private static final String TEST_EMAIL = "test_user@example.com";
  private static final UUID TEST_ID = UUID.fromString("f0cfd733-3f41-46a7-b400-c1774dac4b21");
  private static final UUID ADMIN_GROUP_ID =
      UUID.fromString("09d93820-500e-4027-95ec-fdb5ce6986ce");
  private static final UUID SUBMITTER_GROUP_ID =
      UUID.fromString("64541e68-c6c6-469d-9dcf-9aa260ef17ea");
  private static final String SHORT_NAME = "TEST-CA";

  @Before
  public void setUp() {
    val egoUrl = format("http://localhost:%s", wireMockRule.port());
    val egoClientId = "program-service";
    val egoClientSecret = "qa-program-service";

    val testTemplate =
        new RestTemplateBuilder()
            .basicAuthentication(egoClientId, egoClientSecret)
            .setConnectTimeout(Duration.ofSeconds(15))
            .build();
    testTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(egoUrl));

    client =
        new EgoRESTClient(
            lenientRetryTemplate, retryTemplate, testTemplate, CommonConverter.INSTANCE);
    egoService = new EgoService(converter, client, inviteRepository);
  }

  void stub(String url, String filename) {
    stubFor(
        get(urlEqualTo(url))
            .willReturn(
                aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile(filename)));
  }

  public void getUser(String filename) {
    stub(format("/users?query=%s", TEST_EMAIL), filename);
  }

  @Test
  public void mockGetUser_success() {
    getUser("getUser.json");
    assertTrue(client.getUser(TEST_EMAIL).isPresent());
    assertEquals(client.getUser(TEST_EMAIL).get().getEmail(), TEST_EMAIL);
  }

  @Test
  public void mockGetUser_fail() {
    getUser("getUser_empty_result.json");
    assertTrue(client.getUser(TEST_EMAIL).isEmpty());
  }

  public void mockGetUsersByGroupId(UUID groupId, String filename) {
    stub(format("/groups/%s/users", groupId), filename);
  }

  public void getGroupsByUserId(UUID userId, String filename) {
    stub(format("/users/%s/groups", userId), filename);
  }

  public void getGroupsFail(UUID userId) {
    stubFor(
        get(urlEqualTo(format("/users/%s/groups", userId)))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));
  }

  @Test
  public void joinProgram_success() {
    mockGetUser_success();
    val roleMap = mockGetGroupIdByName(SHORT_NAME);
    val egoGroupId = roleMap.get(UserRole.COLLABORATOR);
    mockGetUsersByGroupId(egoGroupId, "userOne.json");

    // mock ego endpoint addUsersToGroup
    stubFor(
        post(urlEqualTo(format("/groups/%s/users", egoGroupId)))
            .willReturn(
                aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody("User is added to group.")));

    // Call the target method
    assertTrue(egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR));
  }

  @Test
  public void join_program_user_not_found_fail() {
    mockGetUser_fail();

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(TEST_EMAIL);
    exceptionRule.expectMessage("user does not exist in ego");
    egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR);
  }

  @Test
  public void join_program_user_already_joined_fail() {
    val groupName = "PROGRAM-" + SHORT_NAME + "-COLLABORATOR";
    val roleToGroupId = mockGetGroupIdByName(SHORT_NAME);
    val egoGroupId = roleToGroupId.get(UserRole.COLLABORATOR);
    mockGetUser_success();
    mockGetUsersByGroupId(egoGroupId, "userOne_userTwo.json");
    exceptionRule.expect(EgoException.class);
    exceptionRule.expectMessage(
        format("User %s has already joined ego group %s (%s).", TEST_EMAIL, egoGroupId, groupName));
    egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR);
  }

  @Test
  public void join_program_cannot_add_user_to_group_fail() {
    mockGetUser_success();
    val roleToGroupId = mockGetGroupIdByName(SHORT_NAME);
    val egoGroupId = roleToGroupId.get(UserRole.COLLABORATOR);
    mockGetUsersByGroupId(egoGroupId, "userOne.json");

    // mock ego endpoint addUsersToGroup
    stubFor(post(format("/groups/%s/users", egoGroupId)).willReturn(aResponse().withStatus(401)));

    exceptionRule.expect(EgoException.class);
    exceptionRule.expectMessage(
        format("Cannot join user %s to program %s", TEST_EMAIL, SHORT_NAME));
    assertTrue(egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR));
  }

  Map<UserRole, UUID> mockGetGroupIdByName(String programShortName) {
    val m = new TreeMap<UserRole, UUID>();
    for (val role : UserRole.values()) {
      val id = UUID.randomUUID();
      val groupName = "PROGRAM-" + programShortName + "-" + role.toString();
      val response = groupIdResponse(groupName, id);
      val url = format("/groups?query=%s", groupName);
      stubFor(
          get(urlEqualTo(url))
              .willReturn(
                  aResponse()
                      .withStatus(OK.value())
                      .withHeader("Content-Type", "application/json")
                      .withBody(response)));

      m.put(role, id);
    }
    return m;
  }

  private String groupIdResponse(String name, UUID result) {
    return format(
        "{\n"
            + "  \"count\": 1,\n"
            + "  \"limit\": 0,\n"
            + "  \"offset\": 0,\n"
            + "  \"resultSet\": [\n"
            + "    {\n"
            + "      \"description\": \"not important\",\n"
            + "      \"id\": \"%s\",\n"
            + "      \"name\": \"%s\",\n"
            + "      \"status\": \"APPROVED\"\n"
            + "    }\n"
            + "  ]\n"
            + "}\n",
        result, name);
  }

  public void list_user_success() {
    val egoGroup1 = UUID.randomUUID();
    val egoGroup2 = UUID.randomUUID();

    mockGetUsersByGroupId(egoGroup1, "userOne.json");
    mockGetUsersByGroupId(egoGroup2, "userTwo.json");

    val expectedUsers = List.of("User.One@example.com", "User.Two@example.com");
    val users = egoService.getUsersInProgram(SHORT_NAME);
    assertEquals(2, users.size());
    users.forEach(
        user -> {
          assertTrue(expectedUsers.contains(user.getEmail().getValue()));
          if (user.getEmail().getValue().equals("User.One@example.com")) {
            assertEquals(UserRole.ADMIN, user.getRole().getValue());
          }
          if (user.getEmail().getValue().equals("User.Two@example.com")) {
            assertEquals(UserRole.SUBMITTER, user.getRole().getValue());
          }
        });
  }

  @Test
  public void testGetGroups() {
    getGroupsByUserId(TEST_ID, "getGroups.json");
    assertEquals(1, client.getGroupsByUserId(TEST_ID).count());

    val g =
        client
            .getGroupsByUserId(TEST_ID)
            .filter(egoGroup -> egoGroup.getName().toLowerCase().contains(SHORT_NAME.toLowerCase()))
            .findFirst();
    assertTrue(g.isPresent());
    val group = g.get();

    assertEquals("TEST-CA-ADMIN", group.getName());
    assertEquals(ADMIN_GROUP_ID, group.getId());
  }

  @Test
  public void removeUser_success() {
    mockGetUser_success();
    getGroupsByUserId(TEST_ID, "getGroups.json");

    stubFor(
        delete(urlEqualTo(format("/groups/%s/users/%s", ADMIN_GROUP_ID, TEST_ID)))
            .willReturn(aResponse().withStatus(OK.value())));

    assertTrue(egoService.leaveProgram(TEST_EMAIL, SHORT_NAME));
  }

  @Test
  public void removeUser_userNotFound() {
    // If there's no user to delete, we've successful put the system in a state where that user does
    // not exist
    mockGetUser_fail();
    getGroupsByUserId(TEST_ID, "getGroups.json");

    stubFor(
        delete(urlEqualTo(format("/groups/%s/users/%s", ADMIN_GROUP_ID, TEST_ID)))
            .willReturn(aResponse().withStatus(OK.value())));

    assertTrue(egoService.leaveProgram(TEST_EMAIL, SHORT_NAME));
  }

  @Test
  public void removeUser_group_not_found() {
    // If the user somehow wasn't set up with any groups, we've successfully removed them from all
    // of them.
    mockGetUser_success();
    getGroupsByUserId(TEST_ID, "getGroups_empty_result.json");
    stubFor(
        delete(urlEqualTo(format("/groups/%s/users/%s", ADMIN_GROUP_ID, TEST_ID)))
            .willReturn(aResponse().withStatus(OK.value())));

    assertTrue(egoService.leaveProgram(TEST_EMAIL, SHORT_NAME));
  }

  @Test
  public void removeUser_multipleGroupsFound() {
    // if the user was somehow set up with multiple groups, we need to be able to get rid of them.
    mockGetUser_success();
    getGroupsByUserId(TEST_ID, "getGroups_multiple.json");
    stubFor(
        delete(urlEqualTo(format("/groups/%s/users/%s", ADMIN_GROUP_ID, TEST_ID)))
            .willReturn(aResponse().withStatus(OK.value())));
    stubFor(
        delete(urlEqualTo(format("/groups/%s/users/%s", SUBMITTER_GROUP_ID, TEST_ID)))
            .willReturn(aResponse().withStatus(OK.value())));

    assertTrue(egoService.leaveProgram(TEST_EMAIL, SHORT_NAME));
  }

  @Test
  public void removeUser_getGroupsFailed() {
    // test error reporting when we can't get the ego groups
    mockGetUser_success();
    getGroupsFail(TEST_ID);

    // exceptionRule.expect(EgoException.class);
    String errMsg = format("Cannot get ego groups for user '%s'", TEST_ID);
    // exceptionRule.expectMessage(errMsg);
    EgoException err = null;
    try {
      egoService.leaveProgram(TEST_EMAIL, SHORT_NAME);
    } catch (EgoException e) {
      err = e;
    }
    assertNotNull(err);
    assertEquals(errMsg, err.getMessage());
  }

  @Test
  public void removeUser_removeFailed() {
    // test error reporting when removing all our groups fails
    mockGetUser_success();

    getGroupsByUserId(TEST_ID, "getGroups_multiple.json");

    stubFor(
        delete(urlEqualTo(format("/groups/%s/users/%s", ADMIN_GROUP_ID, TEST_ID)))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));
    stubFor(
        delete(urlEqualTo(format("/groups/%s/users/%s", SUBMITTER_GROUP_ID, TEST_ID)))
            .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR.value())));

    exceptionRule.expect(EgoException.class);
    String errMsg =
        "Cannot remove user 'f0cfd733-3f41-46a7-b400-c1774dac4b21' from group 'TEST-CA-ADMIN' "
            + "Cannot remove user 'f0cfd733-3f41-46a7-b400-c1774dac4b21' from group 'TEST-CA-SUBMITTER' ";
    exceptionRule.expectMessage(errMsg);
    egoService.leaveProgram(TEST_EMAIL, SHORT_NAME);
  }
}
