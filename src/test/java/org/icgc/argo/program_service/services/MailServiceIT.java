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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan(lazyInit = true)
@Testcontainers
class MailServiceIT {
  @Autowired MailService mailService;

  @Mock JoinProgramInviteEntity invite;

  @Mock ProgramEntity mockProgramEntity;

  // host ports used in test container
  static int MAILHOG_HTTP_PORT = 10200;
  static int MAILHOG_MAIL_PORT = 10300;

  @Container
  static GenericContainer mailhogContainer =
      new FixedHostPortGenericContainer("mailhog/mailhog:v1.0.0")
          .withFixedExposedPort(MAILHOG_HTTP_PORT, 8025) // http port used in test
          .withFixedExposedPort(MAILHOG_MAIL_PORT, 1025) // mail port used by application
          .waitingFor(
              Wait.forHttp("/")); // Define wait condition during startup, checks lowest host port

  String mailHogRootUrl = "http://" + mailhogContainer.getIpAddress() + ":" + MAILHOG_HTTP_PORT;

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
    when(mockProgramEntity.getShortName()).thenReturn("TEST-PR");
    when(mockProgramEntity.getName()).thenReturn("A Test Program Name");
    when(invite.getProgram()).thenReturn(mockProgramEntity);
    when(invite.getExpiresAt()).thenReturn(LocalDateTime.now());
    mailService.sendInviteEmail(invite);
    val messages =
        restTemplate.getForObject(
            mailHogRootUrl + "/api/v2/search?kind=containing&query=" + randomEmail, JsonNode.class);
    assertTrue(messages.at("/total").asInt() > 0);
    assertEquals("noreply", messages.at("/items/0/From/Mailbox").asText());
    assertEquals("icgc-argo.org", messages.at("/items/0/From/Domain").asText());
    assertEquals(
        "You have been invited to join an ICGC ARGO program",
        messages.at("/items/0/Content/Headers/Subject/0").asText());
    assertEquals(randomUserName, messages.at("/items/0/To/0/Mailbox").asText());
    assertEquals("program-service.com", messages.at("/items/0/To/0/Domain").asText());
    assertTrue(messages.at("/items/0/Content/Body").asText().contains("Albert"));
    assertTrue(messages.at("/items/0/Content/Body").asText().contains("Einstein"));
  }
}
