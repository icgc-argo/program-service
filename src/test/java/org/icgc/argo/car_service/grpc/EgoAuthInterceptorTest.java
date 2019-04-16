package org.icgc.argo.car_service.grpc;

import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.val;
import org.icgc.argo.car_service.Car;
import org.icgc.argo.car_service.CarServiceGrpc;
import org.icgc.argo.car_service.CarServiceGrpc.CarServiceImplBase;
import org.icgc.argo.car_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.car_service.services.EgoService;
import org.icgc.argo.car_service.services.EgoService.EgoToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

// GrpcCleanupRule only works with junit 4
@RunWith(MockitoJUnitRunner.class)
public class EgoAuthInterceptorTest {

  @Mock
  private EgoService egoService;

  @Mock
  private EgoToken egoToken;

  private Channel channel;

  private String serverName;

  private EgoToken egoTokenSpy;

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

  @Test
  public void egoAuthInterceptor_setEgoToken() throws Exception {
    CarServiceImplBase programServiceImplBase =
            new CarServiceImplBase() {
              @Override
              public void createCar(Car request, StreamObserver<Car> responseObserver) {
                EgoAuthInterceptorTest.this.egoTokenSpy = EgoAuthInterceptor.EGO_TOKEN.get();
                responseObserver.onNext(Car.getDefaultInstance());
                responseObserver.onCompleted();
              }
            };

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
            .addService(ServerInterceptors.intercept(programServiceImplBase, new EgoAuthInterceptor(egoService))).build().start());

    val jwtClientInterceptor = new JwtClientInterceptor();
    val blockingStub = CarServiceGrpc.newBlockingStub(channel).withInterceptors(jwtClientInterceptor);

    jwtClientInterceptor.token = "123";
    given(egoService.verifyToken("123")).willReturn(Optional.of(egoToken));

    blockingStub.createCar(Car.getDefaultInstance());
    assertNotNull(this.egoTokenSpy);

    given(egoService.verifyToken("321")).willReturn(Optional.empty());
    jwtClientInterceptor.token = "321";
    blockingStub.createCar(Car.getDefaultInstance());
    assertNull(this.egoTokenSpy);
  }

  class JwtClientInterceptor implements ClientInterceptor {
    private String token;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          headers.put(EgoAuthInterceptor.JWT_METADATA_KEY, token);
          super.start(responseListener, headers);
        }
      };
    }
  }

  @Test
  public void egoAuthInterceptor_egoAuthAnnotation() throws IOException {
    CarServiceImplBase target = new CarServiceImplBase() {
      @EgoAuthInterceptor.EgoAuth(typesAllowed = {"ADMIN"})
      public void create(Car request, StreamObserver<Car> responseObserver) {
        responseObserver.onNext(Car.getDefaultInstance());
        responseObserver.onCompleted();
      }
    };
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.setProxyTargetClass(true);
    factory.addAspect(EgoAuthInterceptor.EgoAuth.EgoAuthAspect.class);
    CarServiceImplBase proxy = factory.getProxy();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
            .addService(ServerInterceptors.intercept(proxy, new EgoAuthInterceptor(egoService))).build().start());
    val jwtClientInterceptor = new JwtClientInterceptor();
    val blockingStub = CarServiceGrpc.newBlockingStub(channel).withInterceptors(jwtClientInterceptor);

    try {
      jwtClientInterceptor.token = "123";
      given(egoService.verifyToken("123")).willReturn(Optional.empty());
      blockingStub.createCar(Car.getDefaultInstance());
      fail("Expect an status runtime exception to be thrown");
    } catch (StatusRuntimeException e) {
      assertEquals(e.getStatus(), Status.fromCode(Status.Code.UNAUTHENTICATED));
    }

    try {
      given(egoService.verifyToken("123")).willReturn(Optional.of(egoToken));
      given(egoToken.getType()).willReturn("USER");
      blockingStub.createCar(Car.getDefaultInstance());
      fail("Expect an status runtime exception to be thrown");
    } catch (StatusRuntimeException e) {
      assertEquals(e.getStatus(), Status.fromCode(Status.Code.PERMISSION_DENIED));
    }

    given(egoService.verifyToken("123")).willReturn(Optional.of(egoToken));
    given(egoToken.getType()).willReturn("ADMIN");
    val resp = blockingStub.createCar(Car.getDefaultInstance());
    assertThat(resp, instanceOf(Car.class));

  }
}