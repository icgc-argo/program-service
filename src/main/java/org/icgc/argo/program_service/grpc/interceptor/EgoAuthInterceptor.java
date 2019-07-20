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

package org.icgc.argo.program_service.grpc.interceptor;

import io.grpc.*;
import lombok.val;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

@Component
@Profile("auth")
public class EgoAuthInterceptor implements AuthInterceptor {

  private final EgoService egoService;

  public static final Context.Key<EgoToken> EGO_TOKEN
    = Context.key("egoToken");

  public static final Metadata.Key<String> JWT_METADATA_KEY = Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);

  @Autowired
  public EgoAuthInterceptor(EgoService egoService) {
    this.egoService = egoService;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
    ServerCall<ReqT, RespT> call,
    Metadata metadata,
    ServerCallHandler<ReqT, RespT> next) {
    // You need to implement validateIdentity
    String token = metadata.get(JWT_METADATA_KEY);
    val egoToken = egoService.verifyToken(token);
    Context context = Context.current().withValue(EGO_TOKEN, egoToken.orElse(null));
    return Contexts.interceptCall(context, call, metadata, next);
  }
}

