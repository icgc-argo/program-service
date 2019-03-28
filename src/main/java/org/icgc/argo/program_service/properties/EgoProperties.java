package org.icgc.argo.program_service.properties;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;

/**
 * Ego external configuration
 */
@Component
@ConfigurationProperties(prefix="ego")
@Validated
@Setter @Getter
public class EgoProperties {
  /**
   * Base url of ego
   */
  @NotNull
  private String baseUrl;

  /**
   * Path to the public key used by ego to encrypt jwt token.
   */
  @NotNull
  private String publicKeyPath;
}
