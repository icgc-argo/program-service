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

  /**
   * GRPC can be disabled when doing test
   */
  @NotNull
  private Boolean grpcEnabled;
}

