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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.val;
import org.apache.commons.lang.RandomStringUtils;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ActiveProfiles({"test", "default"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:postgresql://localhost:5432/program_db",
      "spring.datasource.driverClassName=org.postgresql.Driver",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect"
    })
@ComponentScan(lazyInit = true)
class MailServiceIT {
  @Autowired MailService mailService;

  @Mock JoinProgramInviteEntity invite;

  @Mock ProgramEntity mockProgramEntity;

  @Value("${spring.mail.host}")
  String mailhogHost;

  RestTemplate restTemplate = new RestTemplate();

  @Test
  void sendInviteEmail() {
    val uuid = UUID.randomUUID();
    val randomUserName = RandomStringUtils.randomAlphabetic(10);
    val randomEmail = randomUserName + "@program-service.com";
    when(invite.getId()).thenReturn(uuid);
    when(invite.getFirstName()).thenReturn("Albert");
    when(invite.getLastName()).thenReturn("Einstein");
    when(invite.getUserEmail()).thenReturn(randomEmail);
    when(mockProgramEntity.getShortName()).thenReturn("TestProgram");
    when(invite.getProgram()).thenReturn(mockProgramEntity);
    when(invite.getExpiresAt()).thenReturn(LocalDateTime.now());
    mailService.sendInviteEmail(invite);
    val messages =
        restTemplate.getForObject(
            "https://" + mailhogHost + "/api/v2/search?kind=containing&query=" + randomEmail,
            JsonNode.class);
    assertTrue(messages.at("/total").asInt() > 0);
    assertEquals("noreply", messages.at("/items/0/From/Mailbox").asText());
    assertEquals("oicr.on.ca", messages.at("/items/0/From/Domain").asText());
    assertEquals(randomUserName, messages.at("/items/0/To/0/Mailbox").asText());
    assertEquals("program-service.com", messages.at("/items/0/To/0/Domain").asText());
    assertTrue(messages.at("/items/0/Content/Body").asText().contains("Albert"));
    assertTrue(messages.at("/items/0/Content/Body").asText().contains("Einstein"));
  }
}
