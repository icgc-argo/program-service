package org.icgc.argo.program_service.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private final EgoAuthInterceptor egoAuthInterceptor;
  private final ProgramServiceImpl programServiceImpl;


  @Autowired
  public GRpcServerRunner(EgoAuthInterceptor egoAuthInterceptor, ProgramServiceImpl programServiceImpl) {
    this.egoAuthInterceptor = egoAuthInterceptor;
    this.programServiceImpl = programServiceImpl;
  }

  @Override
  public void run(String... args) throws Exception {
    int port = 50051;
    server = ServerBuilder.forPort(port)
            .addService(ServerInterceptors.intercept(programServiceImpl, egoAuthInterceptor))
            .addService(ProtoReflectionService.newInstance())
            .build()
            .start();

    log.info("Server started, listening on " + port);
    startDaemonAwaitThread();
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread = new Thread(()->{
      try {
        GRpcServerRunner.this.server.awaitTermination();
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

