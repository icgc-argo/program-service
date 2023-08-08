/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.icgc.argo.program_service.security;

import io.grpc.Context;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Profile("auth")
public class JWTAuthorizationFilter extends OncePerRequestFilter {

  private final EgoRestSecurity egoSecurity;
  private final EgoClient client;
  private String TOKEN_PREFIX = "Bearer";

  public static final Context.Key<EgoToken> EGO_TOKEN = Context.key("egoToken");

  @Autowired
  public JWTAuthorizationFilter(@NonNull EgoRestSecurity egoSecurity, @NotNull EgoClient client) {
    this.egoSecurity = egoSecurity;
    this.client = client;
  }

  @SneakyThrows
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException, RuntimeException {
    val tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);
    EgoRestSecurity egoSecurity = new EgoRestSecurity(client.getPublicKey());
    val egoToken = egoSecurity.verifyRestTokenHeader(removeTokenPrefix(tokenPayload));
    filterChain.doFilter(request, response);
  }

  private String removeTokenPrefix(String token) {
    return token.replace(TOKEN_PREFIX, "").trim();
  }
}
