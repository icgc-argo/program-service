package org.icgc.argo.program_service.properties;

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
 */

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.app.VelocityEngine;
import org.icgc.argo.program_service.services.EgoAuthorizationService;
import org.icgc.argo.program_service.utils.NoOpJavaMailSender;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

/**
 * Ego external configuration, served as metadata for application.yml
 */
@Slf4j
@Component
@Validated
@Setter @Getter
@ConfigurationProperties(prefix = AppProperties.APP)
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

  /* can be null except for when auth is enabled */
  private String dccAdminPermission;


  @Bean
  @ConditionalOnProperty(
    prefix = APP,
    name = "mail-enabled",
    havingValue = "false")
  public JavaMailSender noOpJavaMailSender() {
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
  @Profile("auth")
  public EgoAuthorizationService egoAuthorizationService() {
    val permission = dccAdminPermission;
    log.info(format("Started EgoAuthorization service with dccAdminPermission='%s'", permission));
    assert permission != null;
    return new EgoAuthorizationService(permission);
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

