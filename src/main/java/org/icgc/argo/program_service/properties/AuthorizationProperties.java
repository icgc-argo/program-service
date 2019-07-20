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

import lombok.extern.slf4j.Slf4j;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.services.AuthorizationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AuthorizationProperties {
  @Bean
  public AuthorizationService authService() {
    return new AuthorizationService(EgoAuthInterceptor.EGO_TOKEN.get());
  }

}

