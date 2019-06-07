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

import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.val;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.CreateProgramResponse;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc.ProgramServiceImplBase;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
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
    ProgramServiceImplBase programServiceImplBase =
            new ProgramServiceImplBase() {
              @Override
              public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
                EgoAuthInterceptorTest.this.egoTokenSpy = EgoAuthInterceptor.EGO_TOKEN.get();
                responseObserver.onNext(CreateProgramResponse.getDefaultInstance());
                responseObserver.onCompleted();
              }
            };

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
            .addService(ServerInterceptors.intercept(programServiceImplBase, new EgoAuthInterceptor(egoService))).build().start());

    val jwtClientInterceptor = new JwtClientInterceptor();
    val blockingStub = ProgramServiceGrpc.newBlockingStub(channel).withInterceptors(jwtClientInterceptor);

    jwtClientInterceptor.token = "123";
    given(egoService.verifyToken("123")).willReturn(Optional.of(egoToken));

    blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
    assertNotNull(this.egoTokenSpy);

    given(egoService.verifyToken("321")).willReturn(Optional.empty());
    jwtClientInterceptor.token = "321";
    blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
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
    ProgramServiceImplBase target = new ProgramServiceImplBase() {
      @EgoAuthInterceptor.EgoAuth(typesAllowed = {"ADMIN"})
      public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
        responseObserver.onNext(CreateProgramResponse.getDefaultInstance());
        responseObserver.onCompleted();
      }
    };
    AspectJProxyFactory factory = new AspectJProxyFactory(target);
    factory.setProxyTargetClass(true);
    factory.addAspect(EgoAuthInterceptor.EgoAuth.EgoAuthAspect.class);
    ProgramServiceImplBase proxy = factory.getProxy();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder.forName(serverName).directExecutor()
            .addService(ServerInterceptors.intercept(proxy, new EgoAuthInterceptor(egoService))).build().start());
    val jwtClientInterceptor = new JwtClientInterceptor();
    val blockingStub = ProgramServiceGrpc.newBlockingStub(channel).withInterceptors(jwtClientInterceptor);

    try {
      jwtClientInterceptor.token = "123";
      given(egoService.verifyToken("123")).willReturn(Optional.empty());
      blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
      fail("Expect an status runtime exception to be thrown");
    } catch (StatusRuntimeException e) {
      assertEquals(e.getStatus(), Status.fromCode(Status.Code.UNAUTHENTICATED));
    }

    try {
      given(egoService.verifyToken("123")).willReturn(Optional.of(egoToken));
      given(egoToken.getType()).willReturn("USER");
      blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
      fail("Expect an status runtime exception to be thrown");
    } catch (StatusRuntimeException e) {
      assertEquals(e.getStatus(), Status.fromCode(Status.Code.PERMISSION_DENIED));
    }

    given(egoService.verifyToken("123")).willReturn(Optional.of(egoToken));
    given(egoToken.getType()).willReturn("ADMIN");
    val resp = blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
    assertThat(resp, instanceOf(CreateProgramResponse.class));

  }
}