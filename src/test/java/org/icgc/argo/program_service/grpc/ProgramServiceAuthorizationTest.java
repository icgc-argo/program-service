package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.testing.GrpcCleanupRule;
import lombok.AllArgsConstructor;
import lombok.val;
import org.hibernate.engine.spi.Managed;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.grpc.interceptor.ExceptionInterceptor;
import org.icgc.argo.program_service.model.entity.CountryEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCountry;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.EgoAuthorizationService;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.useDefaultDateFormatsOnly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToSet;

public class ProgramServiceAuthorizationTest {
  UUID invitationUUID = UUID.randomUUID();
  UUID invitationUUID2 = UUID.randomUUID();
  StringValue invitationId = StringValue.of(invitationUUID.toString());
  StringValue invitationId2 = StringValue.of(invitationUUID2.toString());

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

    /*
    In case we want to generate more tokens to test with...

    -----BEGIN RSA PRIVATE KEY-----
    MIIEpQIBAAKCAQEA2e08i2xE07jzorq8Xm/KnNutxwwHElMFbjz1upGpZTHDfs29oHLd4J9XqjCYzKDkBg0Hs3gZY3AsEQycg+RK9Z7yGepgVZhXs
    zMo3KyCDAmM64P9Qtftlz4AfZmR4ypqsAlnruNMYum0WqWvKGFL85sGlkshemLlEQWuEDFJFvVHiWKq4b4BknU9r+t6QROkrAg6upWYUOaK7ZiIje
    BSLYsDQy5jMiXgM6TYSZuebee7vNqZdm9HeUYis3X22yyU8FvfKfkgDFgCL9w/qIpvv7h48X+XVVH50Uwk0L2PTz7d1ohlhuOTEc71japcrQZtvU6
    IQEA7PtHkbABQsAYjjQIDAQABAoIBAQCxhK+h/vLd6LYF48kXwEaymbwn/SMxiRFOaDfe31K2jN/rxhpmvcsBc6sMhoOhhJnaV/ji97zupGwxAy3nv
    ipVhEFAXQxWDT+7SLxLbfaNaaYyHxVJwuzWG3p41YTiICZB+ZdM/fi2RhtVD8vrv74H1Ut7V/4QXMitogvVQuB/4pJRLcG9nvoASWojOay2PX5I6FH
    olzsRUgH+PRgW0rKKEo0pgl6QxpJrvsgnNexX+WJA8ur/jlVttQwhpPf1VC+6LvlKKsT4zWhOdC87eCkvlpSUO6XY4Qfv84gjp/2vpm5aYTqOdEFx7
    ua40r0bA2PoifdwrFANzARe7aXmZ+5hAoGBAPberrlAmT/q/nFAvQfINoNkXPFbGU5C9R57SoXFcYF0oyvYfrd6HWf0qKWEQkLN5IMsOEcfJoP5lsx
    F/j2lhFshi0LfxhCRjDpWApWKCJMywyNPJms4M44y4LEhW+4JGtp/u2GaDcIuTf3wyauNWBDv9HyNqA7Kh5dhbKBzvX65AoGBAOH8hpNNk4ftKZxKa
    Vu/Uim0FtLFGTCrhdkR10kxixPQsK3sGIyzqyzWaqtdUD+2N2SwsyTdRQc1f51xiJ1XpVpmbli8rIMBsTuqDDzjL9V225A/qeRjGRf3/99asd08oEo
    Ybf7ldfs2KhijxW4+f5uxjb1eA+DtSUnPO/tyEIF1AoGBAKqXMHfVGtEfatoJ2VYSVREwfkVOJUt+W3HH0rRjvs6tMcAvp0jUOpPGbe+KWFtfeYPnP
    7Bt5yiVhU39I/WndbGfmWMJzQ1P9m2tV7XMH6bQEiZJIIxA1udxYvEj0ynG4uaQE4UbdlxzsPNEu6cvUebKWdDj9njaHR5PdUffEtgJAoGAIFEYfaA
    uZNXJiYwqnPAzM7uJOALvo0IkFfKzMshe9yp02apVqGlZJURUZMUnYLUSHtgWBkOOR4WjBkTiIH4UK2VSimYQ1Xs8eSfMMDjc8k3ZADvac8qoIAFbG
    fnCTb0Jvw7XTAhMYuxQAM4KwcU2QnGVr2ruaxAD1wZHsaGSMrECgYEAlnQPAej0zwlYThPOX4um5VOEVhT6xTMZpHXQAXWnzu3r17+roPTZf0De+uW
    vojUY3h5X3ra98Xsq+Ol1wWC4dy2MGncGBOIXdwfhVbg0Jim+PS4BI8GHKC6M4xKnTvsS/RW+klpFvotKj6Ocf4Hzz1f8XY9IxcRyOSjQkKYQbVg=

    -----END RSA PRIVATE KEY-----
     */

    val programEgoGroupRepository = mock(ProgramEgoGroupRepository.class);
    val invitationService = mock(InvitationService.class);
    val egoUser = new EgoUser().setEmail(userId().getValue());
    val badUser = new EgoUser().setEmail("y@invalid.com");
    when(invitationService.acceptInvite(invitationUUID)).thenReturn(egoUser);
    when(invitationService.acceptInvite(invitationUUID2)).thenReturn(badUser);
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
    when(programService.listPrograms()).thenReturn(List.of(entity(), entity2(), entity3()));


    val service = new ProgramServiceImpl(programService, programConverter,
      commonConverter, mockEgoService, invitationService, authorizationService);

    val serverName = InProcessServerBuilder.generateName();
    ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    val authInterceptor = new EgoAuthInterceptor(egoService);
    val exceptionInterceptor = new ExceptionInterceptor();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
      InProcessServerBuilder.forName(serverName)
        .directExecutor()
        .addService(ServerInterceptors.intercept(service, exceptionInterceptor, authInterceptor))
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


  AuthorizationTest runTests(String testName, ProgramServiceGrpc.ProgramServiceBlockingStub client) {
    val tests = List.of(
      EndpointTest.of("wrongEmail", () -> client.joinProgram(badJoinProgramRequest())), // 0 -- No one

      EndpointTest.of("createProgam", () -> client.createProgram(createProgramRequest())), // 1 -3 DCC Admin
      EndpointTest.of("updateProgram", () -> client.updateProgram(updateProgramRequest())),
      EndpointTest.of("removeProgram", () -> client.removeProgram(removeProgramRequest())),

      EndpointTest.of("inviteUser", () -> client.inviteUser(inviteUserRequest())),      // 4-7 Program Admin
      EndpointTest.of("updateUser", () -> client.updateUser(updateUserRequest())),
      EndpointTest.of("listUser", () -> client.listUsers(listUsersRequest())),
      EndpointTest.of("removeUser", () -> client.removeUser(removeUserRequest())),

      EndpointTest.of("getProgram", () -> client.getProgram(getProgramRequest())),    //8  Program User
      EndpointTest.of("listProgram",() -> client.listPrograms(Empty.getDefaultInstance())), // 9-10 Public
      EndpointTest.of("joinProgram", () -> client.joinProgram(joinProgramRequest()))
    );


    val t = new AuthorizationTest(testName, tests);
    t.run();

    return t;
  }

  void closeChannel(Channel channel) {
    if (channel instanceof ManagedChannel) {
      ((ManagedChannel) channel).shutdownNow();
      try {
        ((ManagedChannel) channel).awaitTermination(30, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  @Test
  void noAuthentication() throws Exception {
    // no authentication token (should fail for all calls with status UNAUTHENTICATED)
    val c = getClient();

    val tests = runTests("NoAuthentication", c);
    assert tests.threwStatusException(Status.UNAUTHENTICATED);
    closeChannel(c.getChannel());

  }

  @Test
  void expired() throws Exception {
    // expired token (should fail all calls with status UNAUTHORIZED)
    val c= getClient();
    val client = addAuthHeader(c, expiredToken());

    val tests = runTests("ExpiredToken", client);
    closeChannel(c.getChannel());
    assert tests.threwStatusException(Status.PERMISSION_DENIED);
  }

  @Test
  void invalid() throws Exception {
    // invalid (non-parseable) token (should fail all calls with status UNAUTHORIZED)
    val c = getClient();
    val client = addAuthHeader(c, invalidToken());

    val tests = runTests("InvalidToken", client);
    closeChannel(c.getChannel());
    assert tests.threwStatusException(Status.PERMISSION_DENIED);
  }

  @Test
  void wrongKey() throws Exception {
    // DCCAdmin level authentication -- signed with an invalid key
    val c= getClient();
    val tests = runTests("WrongKey",addAuthHeader(c, tokenWrongKey()) );
    closeChannel(c.getChannel());

    assert tests.threwStatusException(Status.PERMISSION_DENIED);
  }

  @Test
  void noPermissions() throws Exception {
    // User token for a user with no permissions yet.
    val c = getClient();
    val client = addAuthHeader(c, tokenNoPermissions());
    val tests = runTests("NoPermissions", client);

    assert tests.threwStatusException(Status.PERMISSION_DENIED,0,9);
    assert tests.threwNoExceptions(9);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assert programs.getProgramsCount() == 0;
  }

  @Test
  void wrongAdmin() throws Exception {
    // Admin level access to a different program shouldn't give us anything.
    val c = getClient();
    val client = addAuthHeader(c, tokenAdminUserWrongProgram());
    val tests = runTests("WrongAdmin", client);

    assert tests.threwStatusException(Status.PERMISSION_DENIED,0,9);
    assert tests.threwNoExceptions(9);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    assert programs.getProgramsCount() == 1;
    closeChannel(c.getChannel());
    assertThat(programs.getProgramsList().get(0).getProgram().getShortName().getValue()).isEqualTo("TEST-DK");
  }

  @Test
  void DCCAdmin() throws Exception {
    // DCCAdmin level authentication -- signed with an invalid key
    val c = getClient();
    val client = addAuthHeader(c, tokenDCCAdmin());

    val tests = runTests("DCCAdmin", client);
    assert tests.threwStatusException(Status.PERMISSION_DENIED,0,1);
    assert tests.threwNoExceptions(1);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assertThat(programs.getProgramsCount()).isEqualTo(3);
  }

  @Test
  void programAdmin() throws Exception {
    val c = getClient();
    val client = addAuthHeader(c, tokenAdminUser());

    val tests = runTests("programAdmin", client);
    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 4);
    assert tests.threwNoExceptions(4);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assertThat(programs.getProgramsCount()).isEqualTo(1);
    assertThat(programs.getProgramsList().get(0).getProgram().getShortName().getValue()).isEqualTo("TEST-CA");
  }

  @Test
  void programUser() throws Exception {
    val c = getClient();
    val client = addAuthHeader(c, tokenProgramUser());

    val tests = runTests("programUser", client);
    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 8);
    assert tests.threwNoExceptions(8);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assertThat(programs.getProgramsCount()).isEqualTo(1);
    assertThat(programs.getProgramsList().get(0).getProgram().getShortName().getValue()).isEqualTo("TEST-CA");
  }

  // Wrong Email

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
    return "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6NTA1Mzc4NTYzNSwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6InhAdGVzdC5jb20iLCJlbWFpbCI6InhAdGVzdC5jb20iLCJzdGF0dXMiOiJBUFBST1ZFRCIsImZpcnN0TmFtZSI6IlRlc3QiLCJsYXN0TmFtZSI6IlVzZXIiLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJVU0VSIiwicGVybWlzc2lvbnMiOlsiUFJPR1JBTS1URVNULURLLldSSVRFIiwiUFJPR1JBTS1URVNULURLLlJFQUQiXX19LCJzY29wZSI6W119.eza4_b03Iv9j9eG90dOjfFLcnUswAXHHzS-GRjo0Z5iWhBXIgm-EXbgIGdLfk9y6JQGJ0AzN55SR_ZsM-ZRBdh6ybcbKrxo6Tp0Xb1tWTKyTnscZu-8Fx9X2EipH4HC0dGTjQiQJNOd5UtFmRc9lg52OtXVGzoAXKYp61mNzxBaJ--8paVHRTfWqn0LScjku1oSKzkRSFo2Gg1besJkGelonxNZal8lTtNWf9Y67lcle4s_dwSXCH8Hc1RNpPqxN0hsq8E6EuArx9GAUqVujBsrHOv99FO5ys1Z1iHLcMfNYmIhwOlrRCHDFQTkll0B2of5etyHNqb2SUYdQNWXD4Q";
  }

  StringValue programName() {
    return StringValue.of("TEST-CA");
  }

  StringValue website() {
    return StringValue.of("https://test.ca");
  }

  ProgramEntity entity() {
    val created = LocalDateTime.now();
    val p = new ProgramEntity();
    val c = countries(p, "CA");
    return p.setShortName(programName().getValue()).setName("").setSubmittedDonors(1).
      setCommitmentDonors(10000).setProgramCountries(c).
      setCreatedAt(created).setDescription("Fake").setGenomicDonors(10).
      setMembershipType(MembershipType.ASSOCIATE).setProgramCancers(Set.of()).setProgramPrimarySites(Set.of()).
      setWebsite("http://org.com");
  }

  Set<ProgramCountry> countries(ProgramEntity programEntity, String... names) {
    val n = Arrays.asList(names);
    return mapToSet(n, name -> ProgramCountry.createProgramCountry(programEntity, countryEntity(name)).get());
  }
  CountryEntity countryEntity(String name) {
    val c = new CountryEntity().setName(name).setId(UUID.randomUUID());
    return c;
  }
  ProgramEntity entity2() {
    val created = LocalDateTime.now();
    val p = new ProgramEntity();
    return p.setShortName("TEST-DK").setName("").setSubmittedDonors(2).
      setCommitmentDonors(20000).setProgramCountries(countries(p, "DK")).
      setCreatedAt(created).setDescription("Fake 2").setGenomicDonors(20).
      setMembershipType(MembershipType.ASSOCIATE).setProgramCancers(Set.of()).setProgramPrimarySites(Set.of()).
      setWebsite("http://org.com");
  }

  ProgramEntity entity3() {
    val created = LocalDateTime.now();
    val p = new ProgramEntity();
    return p.setShortName("OTHER-CA").setName("").setSubmittedDonors(3).
      setCommitmentDonors(30000).setProgramCountries(countries(p,"CA")).setCreatedAt(created).setDescription("Fake 3").setGenomicDonors(30).
      setMembershipType(MembershipType.FULL).setProgramCancers(Set.of()).setProgramPrimarySites(Set.of()).
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

  ListUsersRequest listUsersRequest() {
    return ListUsersRequest.newBuilder().setProgramShortName(programName()).build();
  }

  JoinProgramRequest joinProgramRequest() {
    return JoinProgramRequest.newBuilder().setJoinProgramInvitationId(invitationId).build();
  }

  JoinProgramRequest badJoinProgramRequest() {
    return JoinProgramRequest.newBuilder().setJoinProgramInvitationId(invitationId2).build();
  }

  RemoveUserRequest removeUserRequest() {
    return RemoveUserRequest.newBuilder().setProgramShortName(programName()).setUserEmail(userId()).build();
  }

}

// Helper classes

 /**
  * Remote Procedure call test
  */
@AllArgsConstructor
class EndpointTest implements Runnable {
  String rpcName;
  Runnable code;
  Exception exception;

  static EndpointTest of(String rpcName, Runnable code) {
    return new EndpointTest(rpcName, code, null);
  }

  public void run() {
    // Stop GRPC from logging the exception that we're about to catch. We do our own handling; we don't need it
    // screaming irrelevant error messages at level SEVERE when our tests are actually working fine!
    Logger.getLogger("io.grpc").setLevel(Level.OFF);
    try {
      code.run();
    } catch (Exception ex) {
      exception = ex;
    }
    Logger.getLogger("io.grpc").setLevel(Level.INFO);
  }

  boolean threwStatusException(String testName, Status status) {
    if (exception == null) {
      System.err.printf("In test %s, call to %s did not raise an exception (expected) %s\n", testName, rpcName, status);
      return false;
    }
    if (!(exception instanceof StatusRuntimeException)) {
      System.err.printf("In test %s, call to %s threw %s\n", testName, rpcName, exception.getMessage());
      return false;
    }
    val e = (StatusRuntimeException) exception;
    if (e.getStatus() != status) {
      System.err.printf("In test %s, call to %s had status '%s' (%s), not '%s'\n",
        testName, rpcName, e.getStatus().getCode().toString(), e.getMessage(), status);
      return false;
    }
    return true;
  }

  boolean threwNoExceptions(String testName) {
    if (exception != null) {
      System.err.printf("In test %s, call to %s threw %s\n", testName, rpcName, exception.getMessage());
      return false;
    }
    return true;
  }
}

@AllArgsConstructor
class AuthorizationTest implements Runnable {
  String testName;
  List<EndpointTest> tests;

  public void run() {
    tests.stream().forEach(t -> t.run());
  }

  boolean threwNoExceptions() {
    return threwNoExceptions(0, tests.size());
  }

  boolean threwNoExceptions(Integer start) {
    return threwNoExceptions(start, tests.size());
  }

  boolean threwNoExceptions(Integer start, Integer stop) {
    boolean ok = true;
    for (int i = start; i < stop; i++) {
      val test = tests.get(i);
      if (!test.threwNoExceptions(testName)) {
        ok = false;
      }
    }
    return ok;
  }

  boolean threwStatusException(Status expectedStatus) {
    return threwStatusException(expectedStatus, 0, tests.size());
  }

  boolean threwStatusException(Status expectedStatus, Integer start) {
    return threwStatusException(expectedStatus, start, tests.size());
  }

  boolean threwStatusException(Status expectedStatus, Integer start, Integer stop) {
    boolean ok = true;

    for (int i = start; i < stop; i++) {
      val test = tests.get(i);
      if (!test.threwStatusException(testName, expectedStatus)) {
        ok = false;
      }
    }
    return ok;
  }
}

