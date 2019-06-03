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

package org.icgc.argo.program_service.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.grpc.interceptor.AuthInterceptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(value="app.grpcEnabled")
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private final AuthInterceptor authInterceptor;
  private final ProgramServiceImpl programServiceImpl;
  private final HealthStatusManager healthStatusManager;

  @Value("${app.grpcPort}")
  private Integer port;

  @Autowired
  public GRpcServerRunner(ProgramServiceImpl programServiceImpl, AuthInterceptor authInterceptor) {
    this.programServiceImpl = programServiceImpl;
    this.authInterceptor = authInterceptor;
    this.healthStatusManager = new HealthStatusManager();
  }

  @Override
  public void run(String... args) throws Exception {
    // Interceptor bean depends on run profile.
    val programService = ServerInterceptors.intercept(programServiceImpl, authInterceptor);
    healthStatusManager.setStatus("program_service.ProgramService", ServingStatus.SERVING);

    try {
      server = ServerBuilder.forPort(port)
              .addService(programService)
              .addService(ProtoReflectionService.newInstance())
              .addService(healthStatusManager.getHealthService())
              .build()
              .start();
    } catch (IOException e) {
      log.error("gRPC server cannot be started", e);
    }

    log.info("gRPC Server started, listening on port " + port);
    startDaemonAwaitThread();
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread = new Thread(()->{
      try {
        this.server.awaitTermination();
      } catch (InterruptedException e) {
        log.error("gRPC server stopped.", e);
      }
    });
    awaitThread.start();
  }

  @Override
  final public void destroy() throws Exception {
    log.info("Shutting down gRPC server ...");
    Optional.ofNullable(server).ifPresent(Server::shutdown);
    log.info("gRPC server stopped.");
  }
}

