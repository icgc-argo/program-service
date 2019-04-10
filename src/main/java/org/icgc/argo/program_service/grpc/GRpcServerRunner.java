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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private final AuthInterceptor authInterceptor;
  private final ProgramServiceImpl programServiceImpl;
  private final HealthStatusManager healthStatusManager;


  @Autowired
  public GRpcServerRunner(ProgramServiceImpl programServiceImpl, AuthInterceptor authInterceptor) {
    this.programServiceImpl = programServiceImpl;
    this.authInterceptor = authInterceptor;
    this.healthStatusManager = new HealthStatusManager();
  }

  @Override
  public void run(String... args) throws Exception {
    int port = 50051;

    // Interceptor bean depends on run profile.
    val programService = ServerInterceptors.intercept(programServiceImpl, authInterceptor);
    healthStatusManager.setStatus("program_service.ProgramService", ServingStatus.SERVING);

    server = ServerBuilder.forPort(port)
            .addService(programService)
            .addService(ProtoReflectionService.newInstance())
            .addService(healthStatusManager.getHealthService())
            .build()
            .start();

    log.info("Server started, listening on " + port);
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

