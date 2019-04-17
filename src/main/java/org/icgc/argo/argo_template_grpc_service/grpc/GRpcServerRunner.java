package org.icgc.argo.argo_template_grpc_service.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.icgc.argo.argo_template_grpc_service.grpc.interceptor.AuthInterceptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Slf4j
@Component
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private final AuthInterceptor authInterceptor;
//  private final ProgramServiceImplBase templateCarServiceImpl;

  @Autowired
  public GRpcServerRunner(
//      ProgramServiceImplBase templateCarServiceImpl,
      AuthInterceptor authInterceptor) {
//    this.templateCarServiceImpl = templateCarServiceImpl;
    this.authInterceptor = authInterceptor;
  }

  @Override
  public void run(String... args) throws Exception {
    int port = 50051;

    // Interceptor bean depends on run profile.
//    val carService = ServerInterceptors.intercept(templateCarServiceImpl, authInterceptor);

    server = ServerBuilder.forPort(port)
//            .addService(carService)
            .addService(ProtoReflectionService.newInstance())
            .build()
            .start();

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
    if (!isNull(server)){
      server.shutdown();
    }
    log.info("gRPC server stopped.");
  }
}

