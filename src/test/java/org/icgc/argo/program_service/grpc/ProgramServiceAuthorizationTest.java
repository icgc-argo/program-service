package org.icgc.argo.program_service.grpc;

import com.google.protobuf.StringValue;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.testing.GrpcCleanupRule;
import lombok.val;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.grpc.interceptor.ExceptionInterceptor;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.EgoAuthorizationService;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProgramServiceAuthorizationTest {
  UUID invitationUUID = UUID.randomUUID();
  StringValue invitationId = StringValue.of(invitationUUID.toString());

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private ProgramServiceGrpc.ProgramServiceBlockingStub getClient() throws IOException {
    val authorizationService = new EgoAuthorizationService();
    val key = "-----BEGIN PUBLIC KEY-----\n"
      + "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2e08i2xE07jzorq8Xm/K\n"
      + "nNutxwwHElMFbjz1upGpZTHDfs29oHLd4J9XqjCYzKDkBg0Hs3gZY3AsEQycg+RK\n"
      + "9Z7yGepgVZhXszMo3KyCDAmM64P9Qtftlz4AfZmR4ypqsAlnruNMYum0WqWvKGFL\n"
      + "85sGlkshemLlEQWuEDFJFvVHiWKq4b4BknU9r+t6QROkrAg6upWYUOaK7ZiIjeBS\n"
      + "LYsDQy5jMiXgM6TYSZuebee7vNqZdm9HeUYis3X22yyU8FvfKfkgDFgCL9w/qIpv\n"
      + "v7h48X+XVVH50Uwk0L2PTz7d1ohlhuOTEc71japcrQZtvU6IQEA7PtHkbABQsAYj\n"
      + "jQIDAQAB\n"
      + "-----END PUBLIC KEY-----";
    val publicKey = (RSAPublicKey) Utils.getPublicKey(key, "RSA");

    val programEgoGroupRepository = mock(ProgramEgoGroupRepository.class);
    val invitationService = mock(InvitationService.class);
    when(invitationService.getInvitation(invitationUUID)).thenReturn(joinProgramInvite());
    when(invitationService.inviteUser(entity(), userId().getValue(), "TEST", "USER",
      UserRole.COLLABORATOR)).thenReturn(invitationUUID);

    val programConverter = ProgramConverter.INSTANCE;
    val commonConverter = CommonConverter.INSTANCE;
    val restClient = mock(EgoRESTClient.class);

    val invitationRepository = mock(JoinProgramInviteRepository.class);
    val egoService = new EgoService(programEgoGroupRepository, programConverter, restClient,
      invitationRepository, publicKey);
    val mockEgoService = mock(EgoService.class);

    val programService = mock(ProgramService.class);

    when(programService.createProgram(any())).thenReturn(entity());
    when(programService.getProgram(programName().getValue())).thenReturn(entity());

    val service = new ProgramServiceImpl(programService, programConverter,
      commonConverter, mockEgoService, invitationService, authorizationService);

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

  <T> List<T> list(T... values) {
    return Arrays.asList(values);
  }

  List<Exception> runTests(ProgramServiceGrpc.ProgramServiceBlockingStub client) {
    return list(
      catchException(() -> client.createProgram(createProgramRequest())),
      catchException(() -> client.updateProgram(updateProgramRequest())),
      catchException(() -> client.removeProgram(removeProgramRequest())),
      //catchException(() -> client.listPrograms(Empty.getDefaultInstance())), // always succeeds, contents vary
      catchException(() -> client.inviteUser(inviteUserRequest())),
      catchException(() -> client.updateUser(updateUserRequest())),
      catchException(() -> client.listUser(listUserRequest())),
      catchException(() -> client.removeUser(removeUserRequest())),
      catchException(() -> client.getProgram(getProgramRequest())),
      catchException(() -> client.joinProgram(joinProgramRequest()))
    );
  }

  String[] calls = { "createProgram", "updateProgram", "removeProgram", //"listPrograms",
    "inviteUser",  "updateUser", "listUser", "removeUser", "getProgram", "joinProgram"};

  @Test
  void noAuthentication() throws Exception {
    // no authentication token (should fail for all calls with status UNAUTHENTICATED)
    val client = getClient();
    checkResults("NoAuthentication", runTests(client), Status.UNAUTHENTICATED);
  }

  @Test
  void expired() throws Exception {
    // expired token (should fail all calls with status UNAUTHORIZED)
    val client = addAuthHeader(getClient(), expiredToken());
    checkResults("ExpiredToken", runTests(client), Status.PERMISSION_DENIED);
  }


  @Test
  void invalid() throws Exception {
    // invalid (non-parseable) token (should fail all calls with status UNAUTHORIZED)
    val client = addAuthHeader(getClient(), invalidToken());
    checkResults("InvalidToken", runTests(client), Status.PERMISSION_DENIED);
  }


  @Test
  void wrongKey() throws Exception {
    // DCCAdmin level authentication -- signed with an invalid key
    val client = addAuthHeader(getClient(), tokenWrongKey());
    checkResults("InvalidSignature", runTests(client), Status.PERMISSION_DENIED);
  }

  @Test
  void noPermissions() throws Exception {
    val client = addAuthHeader(getClient(), tokenNoPermissions());
    var results = runTests(client);
    checkResults("NoPermissions", results, Status.PERMISSION_DENIED, 0, 7);
    checkSuccess("NoPermissions", results, 8);
  }

  @Test
  void wrongAdmin() throws Exception {
    // Admin level access to a different program shouldn't give us anything.
    val client = addAuthHeader(getClient(), tokenAdminUserWrongProgram());
    checkResults("WrongAdmin", runTests(client), Status.PERMISSION_DENIED);
  }

  @Test
  void DCCAdmin() throws Exception {
    // DCCAdmin level authentication -- signed with an invalid key
    val client = addAuthHeader(getClient(), tokenDCCAdmin());
    val results = runTests(client);
    boolean ok=true;
    for(int i=0; i < results.size(); i++) {
      val ex = results.get(i);
      if (ex != null) {
        System.err.printf("In test DCCAdmin, call to %s threw %s\n", calls[i], ex.getMessage());
        ok = false;
      }
    }
    assertThat(ok).isTrue();
  }

  @Test
  void programAdmin() throws Exception {
    val client = addAuthHeader(getClient(), tokenAdminUser());
    val results = runTests(client);
    checkResults("programUser", results, Status.PERMISSION_DENIED, 0, 2);
    boolean ok=true;
    for(int i=3; i < results.size(); i++) {
      val ex = results.get(i);
      if (ex != null) {
        System.err.printf("In test programAdmin, call to %s threw %s\n", calls[i], ex.getMessage());
        ok = false;
      }
    }
    assertThat(ok).isTrue();
  }

  @Test
  void programUser() throws Exception {
    val client = addAuthHeader(getClient(), tokenProgramUser());
    val results = runTests(client);
    checkResults("programUser", results, Status.PERMISSION_DENIED, 0, 6);

    boolean ok=true;
    for(int i=7; i < results.size(); i++) {
      val ex = results.get(i);
      if (ex != null) {
        System.err.printf("In test programUser, call to %s threw %s\n", calls[i], ex.getMessage());
        ok = false;
      }
    }
    assertThat(ok).isTrue();
  }

  boolean checkStatus(String testName, String rpcName, Exception ex, Status status) {
    if ( ex == null) {
      System.err.printf("In test %s, call to %s did not raise an exception (expected) %s\n", testName, rpcName, status);
      return false;
    }
    if (!(ex instanceof StatusRuntimeException)){
      System.err.printf("In test %s, call to %s threw %s\n", testName, rpcName, ex.getMessage());
      return false;
    }
    val e = (StatusRuntimeException) ex;
    if (e.getStatus() != status) {
      System.err.printf("In test %s, call to %s had status '%s' (%s), not '%s'\n",
        testName, rpcName, e.getStatus().getCode().toString(), e.getMessage(),status);
      return false;
    }
    return true;
  }

  void checkSuccess(String testName, List<Exception> result) {
    checkSuccess(testName, result, 0, result.size());
  }
  void checkSuccess(String testName, List<Exception> result, Integer start) {
    checkSuccess(testName, result, start, result.size());
  }
  void checkSuccess(String testName, List<Exception> results, Integer start, Integer stop) {
    boolean ok=true;
    for(int i=start; i < stop; i++) {
      val ex = results.get(i);
      if (ex != null) {
        System.err.printf("In test %s, call to %s threw %s\n", testName, calls[i], ex.getMessage());
        ok = false;
      }
    }
    assertThat(ok).isTrue();
  }
  void checkResults(String testName, List<Exception> results, Status expectedStatus) {
    checkResults(testName, results, expectedStatus, 0, results.size());
  }

  void checkResults(String testName, List<Exception> results, Status expectedStatus, Integer start) {
    checkResults(testName, results, expectedStatus, start, results.size());
  }

  void checkResults(String testName, List<Exception> results, Status expectedStatus, Integer start, Integer stop) {
    boolean ok=true;

    for (int i = start; i < stop; i++) {
      val ex = results.get(i);
      if (!checkStatus(testName, calls[i], ex, expectedStatus)) {
        ok = false;
      }
    }
    assertThat(ok).isTrue();
  }



  // Kinds of Tokens:
  // Run all tests for:
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
  // Wrong Email
  // ListUser, InviteUser, Update User, JoinProgram

  String invalidToken() {
    return "ABCDEFG";
  }

  String expiredToken() {
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MTU1Mzc4NTYzNSwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJVU0VSIiwicGVybWlzc2lvbnMiOlsiUFJPR1JBTS1URVNULUNBLlJFQUQiXX19LCJzY29wZSI6W119.AZmkUfZh5ZHafak7uFRBCVXgiGbA3AT8CvNgy7rOupBEi14wqFzrmDNLDA0LFvWKi4mSO6Z-lbRhipnlsSWKjY28d4kvfT8XhkWQoFQJ1Z8S5QOmVKNpg02vTZfOkAKYb9nSSB7Zw8rORZn60S2lNvSYxeTGCgS3uDjKreFTzfNSmHRTJcxpyiLzTkiJAezhE2QOwH4iUNPEcRCYT8TqWaIvnf849pExCeVb6vFg3LKixTJl2Aw69WTt8ujCMyZqURICibWkFRHsgaEhcqEMI5IJW8BnyHouiadiZHWFPcJmEAYMDwkA5z96WeaebDaN_bk5Z_qvgNY_x1hwRmXO_A";
  }

  String tokenWrongKey() {
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MTU1Mzc4NTYzNSwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJBRE1JTiIsInBlcm1pc3Npb25zIjpbXX19LCJzY29wZSI6W119.qkXTFWfGy3zTiYoXuLwt1t_c5480fMpIen1sbYlVCTQtpcdSy4830DPahUACNBuAKptqqmvJb3Dskkrd-ubD-kSR5N23xRUebxw7XT-pRCpWZ2e3Y8z_E_p0ovH4I-QNqfaQ6dYMklQ5xQzyIBnl9xYI3uxebHFowkT8owE1GIl7ff0p29jheOFWdNtG6h5Cmij6L-n_tSFeyDs60nl3prjEzPmGRMIGpt5pB_xAYHCOgkb5uDAnyNRjnxa_cK0ynX4prUnXC7unfQGsJ3e1Ubb8L37ovOaAt3ASWUUjgjjYnHg1-9toVCO5m8miniDF4XPv4l9J9Nrv4ClZgBs-Dw";
  }

  String tokenNoPermissions() {
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MjA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJVU0VSIiwicGVybWlzc2lvbnMiOltdfX0sInNjb3BlIjpbXX0.cIR1aSeuvGzmtztNssWfIv16sJvdHFHwdQNv21MsS-iZsfv28lwsV6zNl1msmVzKS-pnBnJ5BLlUI0bmjt1-DX5-IV4B1EiQ0TlFq4bie1EEKmjc8y_1g7sut0fNQUvMlWlqXpqvvM2w1JXmlCdo2LjLQCqqkC9mzebwm0pBKz6T3u4hA2FxHqZaIVh6-lCraL4RL2Qw0kIePwy7G7djytoteezJmeDGmAHd-sHPG1fz__nxRDP-rSH3J7Y8uYAOTsZbFFqlBh5_XJQ2GEtf1kzxb2TLicXcw-f73Df3RtyZ394jcyTpnftuwo2xO9G_KDOSVOxaAB_DsE19EGF6vg";
  }

  String tokenDCCAdmin() {
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MjA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJBRE1JTiIsInBlcm1pc3Npb25zIjpbXX19LCJzY29wZSI6W119.GjMTZzPzMemM33o2dSGmgxBQN8vT-KZxnl-5tPTzno9XCeNcBNIB-xte93o_MMyE9NxcLYhsQO1C5KMNgbhll9LHLNUU90M-Ez2zqEY-UcOlkDZglA6teyaRUTswA46jlgW8axOTqLh3WpSKUD5t5-2eBNTF6gNyRz_jl9fX5Z7kvzV0h-eus9yWr3jLSi4SfV8lAILFv65JR_20dQ-NCgi4uULoYtFEmJsp4E9xLWR_hpu7EcjyKcXG8Qs_4iK_biXKyBZ8Xtl0oUGojWKoBJdD0Uybvl0Uo5_hkuIfiYspeiohYdC-ls_KWmYUfoUhtB1qRBcYcynMH1AfTOuXjQ";
  }

  String tokenAdminUser() {
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MjA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJVU0VSIiwicGVybWlzc2lvbnMiOlsiUFJPR1JBTS1URVNULUNBLldSSVRFIiwiUFJPR1JBTS1URVNULUNBLlJFQUQiXX19LCJzY29wZSI6W119.RZ2Qobnici__8NhDvMyvuqWD-UCICNeZlSqcEPWwgTg91Xbn04ApzYEi4m28ZGezJkyVwQBAM6iDbxZ1aOzLRJ2aHsnYl-xqX5gVOSbeGVEN_QGLQYF6hPEOfsFLpGCPb8nSfKTM4vmk-f8DELa3eHPC57ANi4pZMVx9ySlSAVFr6DhLgdVEK9d7pRwzHNJ4D3oCz-3bkIzlK6CTI1d6RDIfSQcueof2klOVu1irRc71u-yj3a-AgBiC3eFr27bDsomLgs7RMLbAkVz9tjY8Q0JZrPV8f0zRyTZbxihPYU6vLY1DYmnQcdL5_yZRZevDXVje_HSH5AZrUJ0ZOpgnxA";
  }

  String tokenProgramUser() {
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MjA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJVU0VSIiwicGVybWlzc2lvbnMiOlsiUFJPR1JBTS1URVNULUNBLlJFQUQiXX19LCJzY29wZSI6W119.JGUgOiPoQ6i7W0sGl_-kxoB2QQeazoTjWCSMvDrLdkw7L2QerltR9tN66TVqCvkfnkQuGhvqRkTSTESSWfO4qxNeYbpHhpW9z8XwbSwjF9OR1jWyvte7MDsglckx58bIhKVupsVE6JVmvg4czzxfci4MX4KXLTqiK8ZJTLiz9ylyTNg8Wmy5am0hPoBxdiFOPcx1k41-4EeeMaRt4Ywq4YY2QRHY2ssV_hUuXA4iAUmGhBu9BW8BfnkejUAz9MCO8ikuAShXjRqThHYuvjeD7yscGUtMRqHwxTaO0IuI9Ts6YbRdjpJK1QGaRgPV_jJ97b1klLdzMT71fJUnlGbZLw";
  }

  String tokenAdminUserWrongProgram() {
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MTU1Mzc4NTYzNSwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJVU0VSIiwicGVybWlzc2lvbnMiOlsiUFJPR1JBTS1URVNULURLLldSSVRFIiwiUFJPR1JBTS1URVNULURLLlJFQUQiXX19LCJzY29wZSI6W119.tAgBBaF9L5VVntK-u_Dc1r6mAfrwverdPoj-r6hksM87slkhMNN2Lz3OYZmAyiOyWhhSmGW-r8mXOMGhMTyEsTg3lFue1Xi9zAN88Tyly-myQmj47KqbF8Hetzh37U5flSPE5ki8K-mAlwuFDRDqJThpM9DMQU-De3FYbmYCrLPKuqrFvb3UmCUcyZs9X3_vcOTNlijBW9l7jd6vMHY-DKOMn_CT9MvjnQ8YPIpTNdM38pwBbXzz9bRh-CSeMNuFVCqlpC0flkEcs54CLwJf6JLpY8g3--pq2MY3zfPIClU0NVqfpFDAYCm1JbnPNlSxZ4g-zjD6A1E2-yOZEgzi6Q";
  }

  StringValue programName() {
    return StringValue.of("TEST-CA");
  }

  StringValue website() {
    return StringValue.of("https://test.ca");
  }

  ProgramEntity entity() {
    val created = LocalDateTime.now();
    return new ProgramEntity().setShortName(programName().getValue()).setName("").setSubmittedDonors(3).
      setCommitmentDonors(30000).setCountries("CA").setCreatedAt(created).setDescription("Fake").setGenomicDonors(8).
      setMembershipType(MembershipType.ASSOCIATE).setProgramCancers(Set.of()).setProgramPrimarySites(Set.of()).
    setWebsite("http://org.com");
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
    return User.newBuilder().setFirstName(StringValue.of("TEST")).setLastName(StringValue.of("USER")).
      setEmail(userId()).setRole(roleValue(UserRole.ADMIN)).build();
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
    return InviteUserRequest.newBuilder().setProgramShortName(programName()).setEmail(userId()).
      setFirstName(StringValue.of("TEST")).setLastName(StringValue.of("USER")).
      setRole(roleValue(UserRole.COLLABORATOR)).build();
  }

  UpdateUserRequest updateUserRequest() {
    return UpdateUserRequest.newBuilder().setUserId(userId()).setShortName(programName()).build();
  }

  ListUserRequest listUserRequest() {
    return ListUserRequest.newBuilder().setProgramShortName(programName()).build();
  }


  JoinProgramRequest joinProgramRequest() {
    return JoinProgramRequest.newBuilder().setJoinProgramInvitationId(invitationId).build();
  }

  JoinProgramInvite joinProgramInvite() {
    return new JoinProgramInvite().setUserEmail("x@test.com").setId(invitationUUID).setProgram(entity()).setStatus(
      JoinProgramInvite.Status.PENDING);
  }

  RemoveUserRequest removeUserRequest() {
    return RemoveUserRequest.newBuilder().setProgramShortName(programName()).setUserEmail(userId()).build();
  }

}


