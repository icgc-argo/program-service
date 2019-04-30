package org.icgc.argo.program_service.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

/**
 * Ego external configuration, served as metadata for application.yml
 */
@Component
@ConfigurationProperties(prefix="app")
@Validated
@Setter @Getter
public class AppProperties {
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
}

