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

package org.icgc.argo.program_service.validation;

import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.SneakyThrows;
import lombok.val;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.grpc.interceptor.ExceptionInterceptor;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.CreateProgramResponse;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.io.IOException;
import java.util.HashSet;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// GrpcCleanupRule only works with junit 4
@RunWith(MockitoJUnitRunner.class)
public class ExceptionInterceptorTest {
  private Channel channel;
  private String serverName;
  private ExceptionInterceptor interceptor;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUp() {
    // Generate a unique in-process server name.
    serverName = InProcessServerBuilder.generateName();
    // Create a client channel and register for automatic graceful shutdown.
    channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    interceptor = new ExceptionInterceptor();
  }

  @Test
  public void testHandleException() throws Exception {
    val service = new ProgramServiceGrpc.ProgramServiceImplBase(){
      @Override
      public void createProgram(
        CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
        throw new Error("Everything is wrong!");
      }
    };

    val client = setupTest(service);
    try {
      client.createProgram(CreateProgramRequest.getDefaultInstance());
    } catch (StatusRuntimeException e) {
      assertEquals(e.getStatus().getCode(), Status.Code.INTERNAL);
      assertEquals(e.getStatus().getDescription(), "Everything is wrong!");
      val metadata = e.getTrailers();
      assertEquals(metadata.get(key("name")),"java.lang.Error");
      val stacktrace = metadata.get(key("stacktrace"));
      assertNotNull(stacktrace);
      System.out.println("stacktrace="+stacktrace);
    }

  }

  @Test
  public void testUnwrapsException1() throws Exception {
    verifyUnwrapsException(TransactionSystemException.class);
  }
  @Test
  public void testUnwrapsException2() throws Exception {
    verifyUnwrapsException(javax.persistence.RollbackException.class);
  }

  @Test
  public void testUnwrapsException3() throws Exception {
    verifyUnwrapsException(DataIntegrityViolationException.class);
  }
  @Test
    public void getTestUnwrapsException4() throws Exception {
    verifyUnwrapsException(org.hibernate.exception.ConstraintViolationException.class);
  }

  public void verifyUnwrapsException(Class<? extends Throwable> c) throws Exception {
    Throwable inner = new Error("Everything is wrong!");
    val service = new ProgramServiceGrpc.ProgramServiceImplBase() {

      @SneakyThrows
      @Override
      public void createProgram(
        CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
        val ex = mock(c, "mockClassName");
        when(ex.getCause()).thenReturn(inner);
        throw ex;
      }
    };
    val client = setupTest(service);
    try {
      client.createProgram(CreateProgramRequest.getDefaultInstance());
    } catch (StatusRuntimeException e) {
      assertEquals(e.getStatus().getCode(), Status.Code.INTERNAL);
      assertEquals(e.getStatus().getDescription(), "Everything is wrong!");
      val metadata = e.getTrailers();
      assertEquals(metadata.get(key("name")), "java.lang.Error");
      val stacktrace = metadata.get(key("stacktrace"));
      assertNotNull(stacktrace);
      System.out.println("stacktrace=" + stacktrace);
    }

  }

  @Test
  public void testHandleStatusRuntimeException() throws Exception {
    val service = new ProgramServiceGrpc.ProgramServiceImplBase() {
      @Override
      public void createProgram(
        CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
        throw Status.CANCELLED.augmentDescription("fail").asRuntimeException();
      }
    };

    val client = setupTest(service);
    try {
      client.createProgram(CreateProgramRequest.getDefaultInstance());
    } catch (StatusRuntimeException e) {
      assertEquals(e.getStatus().getCode(), Status.Code.CANCELLED);
      assertEquals(e.getStatus().getDescription(), "fail");
    }
  }

  @Test
  public void testHandleConstraintViolationException() throws Exception {
    val cv1 = mock(ConstraintViolation.class);
    val p1 = mock(Path.class, "Large Bowl");
    when(cv1.getMessage()).thenReturn("TOO_HOT");
    when(cv1.getPropertyPath()).thenReturn(p1);

    val cv2 = mock(ConstraintViolation.class);
    val p2 = mock(Path.class, "Small Bowl");
    when(cv2.getMessage()).thenReturn("TOO_COLD");
    when(cv2.getPropertyPath()).thenReturn(p2);

    val service = new ProgramServiceGrpc.ProgramServiceImplBase() {
      @Override
      public void createProgram(
        CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
        val violations = new HashSet<ConstraintViolation<?>>();
        violations.add(cv1);
        violations.add(cv2);
        val ex = new ConstraintViolationException(violations);
        throw ex;
      }
    };

    val client = setupTest(service);
    try {
      client.createProgram(CreateProgramRequest.getDefaultInstance());
    } catch (StatusRuntimeException e) {
      assertEquals(Status.Code.INVALID_ARGUMENT, e.getStatus().getCode() );
      val desc = e.getStatus().getDescription();
      assertTrue("Large Bowl=>TOO_HOT, Small Bowl=>TOO_COLD".equals(desc) ||
        "Small Bowl=>TOO_COLD, Large Bowl=>TOO_HOT".equals(desc));
    }
  }

  @Test
  public void testHandleNoException() throws IOException {
    val service = new ProgramServiceGrpc.ProgramServiceImplBase() {
      @EgoAuthInterceptor.EgoAuth(typesAllowed = { "ADMIN" })
      public void createProgram(
        CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
        responseObserver.onNext(CreateProgramResponse.getDefaultInstance());
        responseObserver.onCompleted();
      }
    };

    val client = setupTest(service);
    val result = client.createProgram(CreateProgramRequest.getDefaultInstance());
    assertNotNull(result);

  }

  private Metadata.Key<String> key(String s) {
    return Metadata.Key.of(s, Metadata.ASCII_STRING_MARSHALLER);
  }

  private ProgramServiceGrpc.ProgramServiceBlockingStub setupTest(BindableService service) throws IOException {
    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
      InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(ServerInterceptors.intercept(service, interceptor))
        .build()
        .start());

    return ProgramServiceGrpc.newBlockingStub(channel);
  }
}

