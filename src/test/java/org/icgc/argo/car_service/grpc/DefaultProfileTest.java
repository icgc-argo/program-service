package org.icgc.argo.car_service.grpc;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.SneakyThrows;
import lombok.val;
import org.icgc.argo.car_service.Car;
import org.icgc.argo.car_service.CarServiceGrpc;
import org.icgc.argo.car_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.car_service.services.EgoService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@ActiveProfiles("default")
public class DefaultProfileTest {

  private Channel channel;

  private String serverName;

  @Mock
  private EgoService egoService;

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUp() {
    // Generate a unique in-process server name.
    serverName = InProcessServerBuilder.generateName();
    // Create a client channel and register for automatic graceful shutdown.
    channel =
            grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @SneakyThrows
  @Test
  public void egoInterceptorDisabled(){
    val target = new CarServiceGrpc.CarServiceImplBase() {
      @Override
      @EgoAuthInterceptor.EgoAuth(typesAllowed = {"ADMIN"})
      public void createCar(Car request, StreamObserver<Car> responseObserver) {
        responseObserver.onNext(Car.getDefaultInstance());
        responseObserver.onCompleted();
      }
    };

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor().addService(target).build().start());

    val blockingStub = CarServiceGrpc.newBlockingStub(channel);

    //EgoService is not initialized under default profile
    assertNull(egoService);

    //Ego interceptor is not stopping create request, as expected
    val detail = blockingStub.createCar(Car.getDefaultInstance());
    assertTrue(detail.equals(Car.getDefaultInstance()));
  }

}