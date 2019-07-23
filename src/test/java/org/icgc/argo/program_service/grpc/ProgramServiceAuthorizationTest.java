package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.testing.GrpcCleanupRule;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.grpc.interceptor.ExceptionInterceptor;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.EgoAuthorizationService;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@Profile("auth")
@SpringBootTest
@RunWith(SpringRunner.class)
class AuthorizationTest {
    @Autowired
    EgoService egoService;
    @Autowired
    ProgramService programService;
    @Autowired
    InvitationService invitationService;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  private ProgramServiceGrpc.ProgramServiceBlockingStub getClient() throws IOException {
    val authorizationService = new EgoAuthorizationService();

    val service = new ProgramServiceImpl(programService, ProgramConverter.INSTANCE,
      CommonConverter.INSTANCE, egoService, invitationService, authorizationService);

    val serverName = InProcessServerBuilder.generateName();
    val channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    val interceptor = new EgoAuthInterceptor(egoService);
    val interceptor2 = new ExceptionInterceptor();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
      InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(ServerInterceptors.intercept(service, interceptor, interceptor2))
        .build()
        .start());

    return ProgramServiceGrpc.newBlockingStub(channel);
  }

  ProgramServiceGrpc.ProgramServiceBlockingStub addAuthHeader(ProgramServiceGrpc.ProgramServiceBlockingStub client,
    String jwt) {
    val headers = new io.grpc.Metadata();
    val key = io.grpc.Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);
    headers.put(key, jwt);

    return MetadataUtils.attachHeaders(client, headers);
  }

  public Exception catchException(Runnable r) {
    Exception exception = null;
    try {
      r.run();
    } catch (Exception ex) {
      exception = ex;
    }
    return exception;
  }

  List<Exception> runTests(ProgramServiceGrpc.ProgramServiceBlockingStub client) {
    return List.of(
      catchException(()->client.createProgram(createProgramRequest())),
      catchException(()->client.getProgram(getProgramRequest())),
      catchException(()->client.updateProgram(updateProgramRequest())),
      catchException(()->client.removeProgram(removeProgramRequest())),
      catchException(()->client.listPrograms(Empty.getDefaultInstance())),
      catchException(()->client.inviteUser(inviteUserRequest())),
      catchException(()->client.joinProgram(joinProgramRequest())),
      catchException(()->client.updateUser(updateUserRequest())),
      catchException(()->client.listUser(listUserRequest())),
      catchException(()->client.removeUser(removeUserRequest()))
      );
  }

  String[] calls = {"createProgram", "updateProgram", "removeProgram", "listPrograms", "getProgram",
    "inviteUser", "joinProgram","updateUser", "listUser", "removeUser" };

  @Test
  void noAuthentication() throws Exception {
    // no authentication token (should fail for all calls with status UNAUTHENTICATED)
    val client = getClient();
    val results = runTests(client);

    boolean failed=false;

    for (int i=0; i < results.size();i++) {
      val ex = results.get(i);
      assertThat(ex).isInstanceOf(StatusRuntimeException.class);
      val e = (StatusRuntimeException) ex;
      if (e.getStatus() != Status.UNAUTHENTICATED) {
        System.err.printf("In test NoAuthentication, call to %s had status '%s', not 'UNAUTHENTICATED'\n",
          calls[i],e.getStatus().getCode().toString());
        failed=true;
      }
    }
    assertThat(failed).isFalse();
  }

  @Test
  void expiredToken() throws Exception {
    // expired token (should fail all calls with status UNAUTHORIZED)
    val client = addAuthHeader(getClient(), createExpiredToken());
    val results = runTests(client);

    boolean failed=false;

    for (int i=0; i < results.size();i++) {
      val ex = results.get(i);
      assertThat(ex).isInstanceOf(StatusRuntimeException.class);
      val e = (StatusRuntimeException) ex;
      if (e.getStatus() != Status.PERMISSION_DENIED) {
        System.err.printf("In test expiredToken, call to %s had status '%s', not 'PERMISSION_DENIED'\n",
          calls[i],e.getStatus().getCode().toString());
        failed=true;
      }
    }
    assertThat(failed).isFalse();
  }

  @Test
  void invalidToken() throws Exception {
    // invalid (non-parseable) token (should fail all calls with status UNAUTHORIZED)
    val client = addAuthHeader(getClient(), createInvalidToken());
    val results = runTests(client);

    boolean failed=false;

    for (int i=0; i < results.size();i++) {
      val ex = results.get(i);
      assertThat(ex).isInstanceOf(StatusRuntimeException.class);
      val e = (StatusRuntimeException) ex;
      if (e.getStatus() != Status.PERMISSION_DENIED) {
        System.err.printf("In test invalid token, call to %s had status '%s', not 'PERMISSION_DENIED'\n",
          calls[i],e.getStatus().getCode().toString());
        failed=true;
      }
    }
    assertThat(failed).isFalse();
  }

  @Test
  void invalidSignature() throws Exception {
    val client = addAuthHeader(getClient(), createTokenWithWrongSignature());
    val results = runTests(client);

    boolean failed=false;

    for (int i=0; i < results.size();i++) {
      val ex = results.get(i);
      assertThat(ex).isInstanceOf(StatusRuntimeException.class);
      val e = (StatusRuntimeException) ex;
      if (e.getStatus() != Status.PERMISSION_DENIED) {
        System.err.printf("In test invalidSignature, call to %s had status '%s', not 'PERMISSION_DENIED'\n",
          calls[i],e.getStatus().getCode().toString());
        failed=true;
      }
    }
    assertThat(failed).isFalse();
  }

    // Kinds of Tokens:
    // Run all tests for:
    // invalid signature (should fail all auth)
    // hasDCCAdmin()
    // hasEmail()
    // hasAdmin()
    // hasMember()
    // hasNothing()

    // Kinds of Tests
    // CreateProgram() (new program)
    // UpdateProgram() (exists)
    // RemoveProgram() (exists)
    //
    // ListPrograms() => Includes own programs, doesn't include others
    // Owner has 0, 1, many programs
    // GetProgram() (exists)
    //
    // ListUser, InviteUser, Update User, JoinProgram,

    String createInvalidToken() {
      return "ABCDEFG";
    }

    String createExpiredToken() {
      throw new RuntimeException("Implement me");
    }

    String createTokenWithWrongSignature() {
      throw new RuntimeException("Implement me");
    }


    StringValue programName() {
      return StringValue.of("TEST-CA");
    }

    StringValue website() {
      return StringValue.of("https://test.ca");
    }

    Program program() {
      return Program.newBuilder().
        setName(programName()).
        setShortName(programName()).
        setWebsite(website()).
        build();
    }

    StringValue userId() {
      return StringValue.of("x@test.com");
    }

    UserRoleValue roleValue(UserRole role) {
      return UserRoleValue.newBuilder().setValue(role).build();
    }

    User user() {
      return User.newBuilder().setEmail(userId()).setRole(roleValue(UserRole.ADMIN)).build();
    }

    CreateProgramRequest createProgramRequest() {
      return CreateProgramRequest.newBuilder().setProgram(program()).addAdmins(user()).build();
    }

    UpdateProgramRequest updateProgramRequest() {
      return UpdateProgramRequest.newBuilder().setProgram(program()).build();
    }

    RemoveProgramRequest removeProgramRequest() {
      return RemoveProgramRequest.newBuilder().setProgramShortName(programName()).build();
    }

    GetProgramRequest getProgramRequest() {
      return GetProgramRequest.newBuilder().setShortName(programName()).build();
    }

    InviteUserRequest inviteUserRequest() {
      return InviteUserRequest.newBuilder().setProgramShortName(programName()).build();
    }

    UpdateUserRequest updateUserRequest() {
      return UpdateUserRequest.newBuilder().setUserId(userId()).setRole(roleValue(UserRole.SUBMITTER)).build();
    }

    ListUserRequest listUserRequest() {
      return ListUserRequest.newBuilder().setProgramShortName(programName()).build();
    }

    StringValue invitationId() {
      return StringValue.of("123");
    }

    JoinProgramRequest joinProgramRequest() {
      return JoinProgramRequest.newBuilder().setJoinProgramInvitationId(invitationId()).build();
    }

    RemoveUserRequest removeUserRequest() {
      return RemoveUserRequest.newBuilder().setProgramShortName(programName()).setUserEmail(userId()).build();
    }

  }


