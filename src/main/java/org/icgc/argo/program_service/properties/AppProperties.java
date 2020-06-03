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

package org.icgc.argo.program_service.properties;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.app.VelocityEngine;
import org.icgc.argo.program_service.services.auth.EgoAuthorizationService;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.utils.NoOpJavaMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/** Ego external configuration, served as metadata for application.yml */
@Slf4j
@Component
@Validated
@Setter
@Getter
@ConfigurationProperties(prefix = AppProperties.APP)
public class AppProperties {
  public static final String APP = "app";
  /** Ego api url */
  @NotNull private String egoUrl;

  /** Ego client Id, it has to be manually added in ego */
  @NotNull private String egoClientId;

  /** Ego client secret */
  @NotNull private String egoClientSecret;

  /** Port used by grpc server */
  @NotNull private Integer grpcPort;

  /** GRPC can be disabled when doing test */
  @NotNull private Boolean grpcEnabled;

  /** Emailing can be disabled when developing/testing */
  @NotNull private Boolean mailEnabled;

  @NotNull private EmailProperties email = new EmailProperties();

  @NotNull private DacoPermissionProperties dacoApprovedPermission = new DacoPermissionProperties();

  /* can be null except for when auth is enabled */
  private String dccAdminPermission;

  @Bean
  @ConditionalOnProperty(prefix = APP, name = "mail-enabled", havingValue = "false")
  public JavaMailSender noOpJavaMailSender() {
    checkArgument(
        !mailEnabled, "The config 'mail-enabled' was 'true' but was expected to be 'false'");
    log.warn("Loaded {}", NoOpJavaMailSender.class.getSimpleName());
    return new NoOpJavaMailSender();
  }

  @Bean
  public VelocityEngine velocityEngine() {
    Properties props = new Properties();
    props.put("resource.loader", "class");
    props.put(
        "class.resource.loader.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    return new VelocityEngine(props);
  }

  @Bean
  @Profile("auth")
  public EgoAuthorizationService egoAuthorizationService() {
    val permission = dccAdminPermission;
    log.info(format("Started EgoAuthorization service with dccAdminPermission='%s'", permission));
    assert permission != null;
    return new EgoAuthorizationService(permission);
  }

  @Bean
  public RestTemplate RestTemplate() {
    val t =
        new RestTemplateBuilder()
            .basicAuthentication(getEgoClientId(), getEgoClientSecret())
            .setConnectTimeout(Duration.ofSeconds(15))
            .build();
    t.setUriTemplateHandler(new DefaultUriBuilderFactory(getEgoUrl()));
    return t;
  }

  @Bean
  public RSAPublicKey egoPublicKey(EgoClient egoClient) {
    return egoClient.getPublicKey();
  }

  @Validated
  @Setter
  @Getter
  public static class EmailProperties {
    @NotNull private String from;

    @NotNull private final InvitationProperties invitation = new InvitationProperties();

    @Validated
    @Setter
    @Getter
    public static class InvitationProperties {
      @NotNull private String invitationUrlPrefix;
      @NotNull private String platformUrl;
      @NotNull private String subject;
      @NotNull private String dacoLink;
      @NotNull private String programAccessDocsLink;
      @NotNull private String docLink;
      @NotNull private String contactLink;
      @NotNull private String privacyPolicyLink;
    }
  }

  @Validated
  @Setter
  @Getter
  public static class DacoPermissionProperties {
    @NotNull private String policyName;
    @NotNull private List<String> accessLevels;
  }
}
