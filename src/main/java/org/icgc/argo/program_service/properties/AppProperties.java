package org.icgc.argo.program_service.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.app.VelocityEngine;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.utils.NoOpJavaMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Ego external configuration, served as metadata for application.yml
 */
@Slf4j
@Component
@Validated
@Setter @Getter
@ConfigurationProperties(prefix= AppProperties.APP)
public class AppProperties {
  public static final String APP = "app";
  /**
   * Url prefix of the invite link, it should be followed by invite's uuid
   */
  @NotNull
  private String invitationUrlPrefix;

  /**
   * Ego api url
   */
  @NotNull
  private String egoUrl;

  /**
   * Ego client Id, it has to be manually added in ego
   */
  @NotNull
  private String egoClientId;

  /**
   * Ego client secret
   */
  @NotNull
  private String egoClientSecret;

  /**
   * Port used by grpc server
   */
  @NotNull
  private Integer grpcPort;

  /**
   * GRPC can be disabled when doing test
   */
  @NotNull
  private Boolean grpcEnabled;

  /**
   * Emailing can be disabled when developing/testing
   */
  @NotNull
  private Boolean mailEnabled;

  @Bean
  @ConditionalOnProperty(
      prefix = APP,
      name = "mail-enabled",
      havingValue = "false")
  public JavaMailSender noOpJavaMailSender(){
    checkArgument(!mailEnabled, "The config 'mail-enabled' was 'true' but was expected to be 'false'");
    log.warn("Loaded {}", NoOpJavaMailSender.class.getSimpleName());
    return new NoOpJavaMailSender();
  }

  @Bean
  public VelocityEngine velocityEngine() {
    Properties props = new Properties();
    props.put("resource.loader", "class");
    props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    return new VelocityEngine(props);
  }

  @Bean
  public RestTemplate RestTemplate() {
    val t = new RestTemplateBuilder()
      .basicAuthentication(getEgoClientId(), getEgoClientSecret())
      .setConnectTimeout(Duration.ofSeconds(15)).
        build();
    t.setUriTemplateHandler(new DefaultUriBuilderFactory(getEgoUrl()));
    return t;
  }
}

