package ca.on.oicr.icgc.argo.program_service.grpc;

import ca.on.oicr.icgc.argo.program_service.GreeterGrpc;
import ca.on.oicr.icgc.argo.program_service.HelloReply;
import ca.on.oicr.icgc.argo.program_service.HelloRequest;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
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

  @Autowired
  public GRpcServerRunner(EgoAuthInterceptor egoAuthInterceptor) {
    this.egoAuthInterceptor = egoAuthInterceptor;
  }

  @Override
  public void run(String... args) throws Exception {
    int port = 50051;
    server = ServerBuilder.forPort(port)
            .addService(ServerInterceptors.intercept(new GreeterImpl(), egoAuthInterceptor))
            .addService(ProtoReflectionService.newInstance())
            .build()
            .start();

    log.info("Server started, listening on " + port);
    server.awaitTermination();
  }

  @Override
  public void destroy() throws Exception {
    log.info("Shutting down gRPC server ...");
    Optional.ofNullable(server).ifPresent(Server::shutdown);
    log.info("gRPC server stopped.");
  }

  static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
//      EgoAuthInterceptor.EGO_TOKEN.get();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    @Override
    public void sayHelloAgain(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
      HelloReply reply = HelloReply.newBuilder().setMessage("Hello again " + req.getName()).build();
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }
  }
}

