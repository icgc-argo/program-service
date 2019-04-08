package org.icgc.argo.program_service.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

/**
 * Ego external configuration, served as metadata for application.yml
 */
@Component
@ConfigurationProperties(prefix="ego")
@Validated
@Setter @Getter
@Profile("auth")
public class EgoProperties {
  /**
   * Url to the public key used by ego to encrypt jwt token.
   */
  @NotNull
  private String publicKeyUrl;
}
