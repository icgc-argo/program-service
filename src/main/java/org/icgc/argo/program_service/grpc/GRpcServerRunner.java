package org.icgc.argo.program_service.grpc;

import io.grpc.*;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private EgoAuthInterceptor egoAuthInterceptor;
  private final ProgramServiceImpl programServiceImpl;


  @Autowired
  public GRpcServerRunner(ProgramServiceImpl programServiceImpl) {
    this.programServiceImpl = programServiceImpl;
  }

  @Autowired(required = false)
  public void setEgoAuthInterceptor(EgoAuthInterceptor egoAuthInterceptor) {
    this.egoAuthInterceptor = egoAuthInterceptor;
  }

  @Override
  public void run(String... args) throws Exception {
    int port = 50051;

    val programService =
            egoAuthInterceptor == null ? programServiceImpl.bindService()
                    : ServerInterceptors.intercept(programServiceImpl, egoAuthInterceptor);

    server = ServerBuilder.forPort(port)
            .addService(programService)
            .addService(ProtoReflectionService.newInstance())
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

