package org.icgc.argo.program_service.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles({ "test", "default" })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/program_db",
        "spring.datasource.driverClassName=org.postgresql.Driver",
})
class MailServiceIT {
  @Autowired
  MailService mailService;

  @Mock
  JoinProgramInvite invite;

  @Mock
  ProgramEntity mockProgramEntity;

  @Value("${spring.mail.host}")
  String mailhogHost;

  RestTemplate restTemplate = new RestTemplate();

  @Test
  void sendInviteEmail() {
    val uuid = UUID.randomUUID();
    when(invite.getId()).thenReturn(uuid);
    when(invite.getFirstName()).thenReturn("Albert");
    when(invite.getLastName()).thenReturn("Einstein");
    when(invite.getUserEmail()).thenReturn("it_test@program-service.com");
    when(mockProgramEntity.getShortName()).thenReturn("TestProgram");
    when(invite.getProgram()).thenReturn(mockProgramEntity);
    mailService.sendInviteEmail(invite);
    val messages = restTemplate.getForObject("https://" + mailhogHost + "/api/v2/search?kind=containing&query=" + uuid, JsonNode.class);
    assertThat(messages.at("/total").asInt()).isGreaterThan(0);
    assertThat(messages.at("/items/0/From/Mailbox").asText()).isEqualTo("noreply");
    assertThat(messages.at("/items/0/From/Domain").asText()).isEqualTo("oicr.on.ca");
    assertThat(messages.at("/items/0/To/0/Mailbox").asText()).isEqualTo("it_test");
    assertThat(messages.at("/items/0/To/0/Domain").asText()).isEqualTo("program-service.com");
    assertThat(messages.at("/items/0/To/0/Domain").asText()).isEqualTo("program-service.com");
    assertThat(messages.at("/items/0/Content/Body").asText()).contains("Albert").contains("Einstein");
  }

}