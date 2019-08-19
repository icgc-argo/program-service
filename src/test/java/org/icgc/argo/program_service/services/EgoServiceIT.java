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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
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
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("test")
public class EgoServiceIT {

  EgoRESTClient client;

  @Autowired
  private RetryTemplate lenientRetryTemplate;

  @Autowired
  private RetryTemplate retryTemplate;

  @Autowired
  EntityGenerator entityGenerator;

  @Autowired
  ProgramEgoGroupRepository repository;

  @Autowired
  ProgramConverter converter;

  @Autowired
  JoinProgramInviteRepository inviteRepository;

  private EgoService egoService;

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  private static final String TEST_EMAIL = "test_user@example.com";
  private static final String SHORT_NAME = "TEST-CA";

  @Before
  public void setUp() {
    val egoUrl = format("http://localhost:%s", wireMockRule.port());
    val egoClientId = "program-service";
    val egoClientSecret = "qa-program-service";

    val testTemplate = new RestTemplateBuilder()
            .basicAuthentication(egoClientId, egoClientSecret)
            .setConnectTimeout(Duration.ofSeconds(15)).build();
    testTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(egoUrl));

    client = new EgoRESTClient(lenientRetryTemplate, retryTemplate, testTemplate, CommonConverter.INSTANCE);
    egoService = new EgoService(repository, converter, client, inviteRepository);
  }

  @Test
  public void getUser(){
    stubFor(get(urlEqualTo(format("/users?query=%s", TEST_EMAIL)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("getUser.json")));
    assertTrue(client.getUser(TEST_EMAIL).isPresent());
    assertEquals(client.getUser(TEST_EMAIL).get().getEmail(), TEST_EMAIL);
  }

  @Test
  public void joinProgram_success() {
    val egoGroupId = UUID.randomUUID();
    entityGenerator.setUpProgramEgoGroupEntity(SHORT_NAME, UserRole.COLLABORATOR, egoGroupId);

    // Mock ego endpoint getUser
    stubFor(get(urlEqualTo(format("/users?query=%s", TEST_EMAIL)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("getUser.json")));
    assertTrue(client.getUser(TEST_EMAIL).isPresent());
    assertEquals(client.getUser(TEST_EMAIL).get().getEmail(), TEST_EMAIL);

    //mock ego endpoint getUserByGroupId
    stubFor(get(urlEqualTo(format("/groups/%s/users", egoGroupId)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("userOne.json")));

    // mock ego endpoint addUsersToGroup
    stubFor(post(urlEqualTo(format("/groups/%s/users", egoGroupId)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBody("User is added to group.")));

    // Call the target method
    assertTrue(egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR));
  }

  @Test
  public void join_program_user_not_found_fail(){
    stubFor(get(urlEqualTo(format("/users?query=%s", TEST_EMAIL)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("getUser_empty_result.json")));
    assertTrue(client.getUser(TEST_EMAIL).isEmpty());
    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(TEST_EMAIL);
    exceptionRule.expectMessage("user does not exist in ego");
    egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR);
  }

  @Test
  public void join_program_user_already_joined_fail(){
    val egoGroupId = UUID.randomUUID();
    entityGenerator.setUpProgramEgoGroupEntity(SHORT_NAME, UserRole.COLLABORATOR, egoGroupId);

    // Mock ego endpoint getUser
    stubFor(get(urlEqualTo(format("/users?query=%s", TEST_EMAIL)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("getUser.json")));
    assertTrue(client.getUser(TEST_EMAIL).isPresent());
    assertEquals(client.getUser(TEST_EMAIL).get().getEmail(), TEST_EMAIL);

    //mock ego endpoint getUserByGroupId
    stubFor(get(urlEqualTo(format("/groups/%s/users", egoGroupId)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    // userOne_userTwo.json response has TEST_EMAIL
                    .withBodyFile("userOne_userTwo.json")));
    exceptionRule.expect(EgoException.class);
    exceptionRule.expectMessage(format("User %s has already joined ego group %s.", TEST_EMAIL, egoGroupId));
    egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR);
  }

  @Test
  public void join_program_cannot_add_user_to_group_fail(){
    val egoGroupId = UUID.randomUUID();
    entityGenerator.setUpProgramEgoGroupEntity(SHORT_NAME, UserRole.COLLABORATOR, egoGroupId);

    // Mock ego endpoint getUser
    stubFor(get(urlEqualTo(format("/users?query=%s", TEST_EMAIL)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("getUser.json")));
    assertTrue(client.getUser(TEST_EMAIL).isPresent());
    assertEquals(client.getUser(TEST_EMAIL).get().getEmail(), TEST_EMAIL);

    //mock ego endpoint getUserByGroupId
    stubFor(get(urlEqualTo(format("/groups/%s/users", egoGroupId)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("userOne.json")));

    // mock ego endpoint addUsersToGroup
    stubFor(post(format("/groups/%s/users", egoGroupId))
            .willReturn(aResponse().withStatus(401)));

    exceptionRule.expect(EgoException.class);
    exceptionRule.expectMessage(format("Cannot join user %s to program %s", TEST_EMAIL, SHORT_NAME));
    assertTrue(egoService.joinProgram(TEST_EMAIL, SHORT_NAME, UserRole.COLLABORATOR));
  }

  @Test
  public void list_user_success(){
    val egoGroup1 = UUID.randomUUID();
    val egoGroup2 = UUID.randomUUID();
    entityGenerator.setUpProgramEgoGroupEntity(SHORT_NAME, UserRole.ADMIN, egoGroup1);
    entityGenerator.setUpProgramEgoGroupEntity(SHORT_NAME, UserRole.SUBMITTER, egoGroup2);

    //mock ego endpoint getUserByGroupId
    stubFor(get(urlEqualTo(format("/groups/%s/users", egoGroup1)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("userOne.json")));
    stubFor(get(urlEqualTo(format("/groups/%s/users", egoGroup2)))
            .willReturn(aResponse()
                    .withStatus(OK.value())
                    .withHeader("Content-Type", "application/json")
                    .withBodyFile("userTwo.json")));

    val expectedUsers = List.of("User.One@example.com", "User.Two@example.com");
    val users = egoService.getUsersInProgram(SHORT_NAME);
    assertTrue(users.size() == 2);
    users.forEach(user -> {
      assertTrue(expectedUsers.contains(user.getEmail().getValue()));
      if(user.getEmail().equals("User.One@example.com")){
        assertTrue(user.getRole().getValue().equals(UserRole.ADMIN));
      }
      if(user.getEmail().equals("User.Two@example.com")){
        assertTrue(user.getRole().getValue().equals(UserRole.SUBMITTER));
      }
    });
  }

}
