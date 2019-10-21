package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.testing.GrpcCleanupRule;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AllArgsConstructor;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.grpc.interceptor.ExceptionInterceptor;
import org.icgc.argo.program_service.model.entity.CountryEntity;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCountry;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.security.EgoSecurity;
import org.icgc.argo.program_service.services.EgoAuthorizationService;
import org.icgc.argo.program_service.services.InvitationService;
import org.icgc.argo.program_service.services.ProgramService;
import org.icgc.argo.program_service.services.ValidationService;
import org.icgc.argo.program_service.services.ego.Context;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.icgc.argo.program_service.Utils.generateRSAKeys;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProgramServiceAuthorizationTest {
  private UUID invitationUUID = UUID.randomUUID();
  private UUID invitationUUID2 = UUID.randomUUID();
  private UUID invitationUUID3 = UUID.randomUUID();
  private StringValue invitationId = StringValue.of(invitationUUID.toString());
  private StringValue invitationId2 = StringValue.of(invitationUUID2.toString());
  private Signer signer;
  private RSAPublicKey publicKey;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private void setupKeys() {
    if (publicKey != null) {
      return;
    }

    val pair = generateRSAKeys();
    publicKey = (RSAPublicKey) pair.getPublic();
    signer = new Signer(pair.getPrivate());
  }

  private ProgramServiceGrpc.ProgramServiceBlockingStub getClient() throws IOException {
    setupKeys();
    val authorizationService = new EgoAuthorizationService(dccAdminPermission());

    val invitationService = mock(InvitationService.class);

    when(invitationService.getInvitationById(invitationUUID)).thenReturn(Optional.of(invite1()));
    when(invitationService.getInvitationById(invitationUUID2)).thenReturn(Optional.of(invite2()));
    when(invitationService.getInvitationById(invitationUUID3)).thenReturn(Optional.empty());

    when(invitationService.inviteUser(entity(), userId().getValue(), "TEST", "USER",
      UserRole.COLLABORATOR)).thenReturn(invitationUUID);

    val programConverter = ProgramConverter.INSTANCE;
    val commonConverter = CommonConverter.INSTANCE;

    val egoSecurity = new EgoSecurity(publicKey);

    val mockEgoService = mock(EgoService.class);

    val programService = mock(ProgramService.class);
    when(programService.createProgram(any())).thenReturn(entity());
    when(programService.getProgram(programName().getValue())).thenReturn(entity());
    when(programService.listPrograms()).thenReturn(List.of(entity(), entity2(), entity3()));

    ValidationService v = mock(ValidationService.class);
    when(v.validateCreateProgramRequest(any())).thenReturn(List.of());

    val service = new ProgramServiceImpl(programService, programConverter,
      commonConverter, mockEgoService, invitationService, authorizationService, v);

    val serverName = InProcessServerBuilder.generateName();
    ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    val authInterceptor = new EgoAuthInterceptor(egoSecurity);
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
      EndpointTest.of("listProgram", () -> client.listPrograms(Empty.getDefaultInstance())), // 9-10 Public
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
    val c = getClient();
    val client = addAuthHeader(c, expiredToken());

    val tests = runTests("ExpiredToken", client);
    closeChannel(c.getChannel());
    assert tests.threwStatusException(Status.UNAUTHENTICATED);
  }

  @Test
  void invalid() throws Exception {
    // invalid (non-parseable) token (should fail all calls with status UNAUTHORIZED)
    val c = getClient();
    val client = addAuthHeader(c, invalidToken());

    val tests = runTests("InvalidToken", client);
    closeChannel(c.getChannel());
    assert tests.threwStatusException(Status.UNAUTHENTICATED);
  }

  @Test
  void wrongKey() throws Exception {
    // DCCAdmin level authentication -- signed with an invalid key
    val c = getClient();
    val tests = runTests("WrongKey", addAuthHeader(c, tokenWrongKey()));
    closeChannel(c.getChannel());

    assert tests.threwStatusException(Status.UNAUTHENTICATED);
  }

  @Test
  void noPermissions() throws Exception {
    // User token for a user with no permissions yet.
    val c = getClient();
    val client = addAuthHeader(c, tokenNoPermissions());
    val tests = runTests("NoPermissions", client);

    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 9);
    assert tests.threwNoExceptions(9);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assert programs.getProgramsCount() == 0;
  }


  @Test
  void egoAdmin() throws Exception {
    // Ego's Admin level authentication shouldn't give us anything
    val c = getClient();
    val client = addAuthHeader(c, tokenEgoAdmin());

    val tests = runTests("EgoAdmin", client);
    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 9);
    assert tests.threwNoExceptions(9);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assertEquals(0, programs.getProgramsCount());
  }

  @Test
  void dccAdminWrongKey() throws Exception {
    // DCCAdmin with an invalid signature shouldn't give us anything
    val c = getClient();
    val client = addAuthHeader(c, tokenDCCAdminWrongKey());

    val tests = runTests("DCCAdminWrongKey", client);
    // Wrong key cannot be authenticated
    assert tests.threwStatusException(Status.UNAUTHENTICATED);
    closeChannel(c.getChannel());
  }

  @Test
  void dccAdmin() throws Exception {
    // DCCAdmin should let us to do anything except join an invitation with the wrong email address.
    val c = getClient();
    val client = addAuthHeader(c, tokenDCCAdmin());

    val tests = runTests("DCCAdmin", client);
    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 1); // wrongEmail should fail
    assert tests.threwNoExceptions(1);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assertEquals(3, programs.getProgramsCount());
  }

  @Test
  void wrongAdmin() throws Exception {
    // Admin level access to a different program shouldn't give us anything.
    val c = getClient();
    val client = addAuthHeader(c, tokenAdminUserWrongProgram());
    val tests = runTests("WrongAdmin", client);

    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 9);
    assert tests.threwNoExceptions(9);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    assert programs.getProgramsCount() == 1;
    closeChannel(c.getChannel());
    assertEquals("TEST-DK", programs.getProgramsList().get(0).getProgram().getShortName().getValue());
  }

  @Test
  void programAdmin() throws Exception {
    val c = getClient();
    val client = addAuthHeader(c, tokenAdminUser());

    val tests = runTests("ProgramAdmin", client);
    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 4);
    assert tests.threwNoExceptions(4);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assertEquals(1, programs.getProgramsCount());
    assertEquals("TEST-CA", programs.getProgramsList().get(0).getProgram().getShortName().getValue());
  }

  @Test
  void programUser() throws Exception {
    val c = getClient();
    val client = addAuthHeader(c, tokenProgramUser());

    val tests = runTests("ProgramUser", client);
    assert tests.threwStatusException(Status.PERMISSION_DENIED, 0, 8);
    assert tests.threwNoExceptions(8);

    val programs = client.listPrograms(Empty.getDefaultInstance());
    closeChannel(c.getChannel());
    assertEquals(1, programs.getProgramsCount());
    assertEquals("TEST-CA", programs.getProgramsList().get(0).getProgram().getShortName().getValue());
  }

  @Test
  void testSigner() {
    setupKeys();
    val jwt = signer.getToken("n@ai", "PROGRAM-TEST-CA.WRITE");
    System.err.printf("Token='%s'\n", jwt);

    val egoSecurity = new EgoSecurity(publicKey);
    val egoToken = egoSecurity.verifyToken(jwt);
    assert egoToken.isPresent();
    System.err.printf("egoToken='%s'", egoToken.get());
  }

  String invalidToken() {
    return "ABCDEFG";
  }

  String expiredToken() {
    return signer.getToken(email(), true, false, dccAdminPermission());
  }

  String tokenWrongKey() {
    val pair = generateRSAKeys();
    val wrongSigner = new Signer(pair.getPrivate());
    return wrongSigner.getToken(email(), "PROGRAM-TEST-CA.WRITE");
  }

  String tokenNoPermissions() {
    return signer.getToken(email());
  }

  String tokenEgoAdmin() {
    return signer.getToken(email(), false, true);
  }

  String tokenDCCAdmin() {
    return signer.getToken(email(), dccAdminPermission());
  }

  String tokenDCCAdminWrongKey() {
    val pair = generateRSAKeys();
    val wrongSigner = new Signer(pair.getPrivate());
    return wrongSigner.getToken(email(), dccAdminPermission());
  }

  String tokenAdminUser() {
    return signer.getToken(email(), "PROGRAM-TEST-CA.WRITE");
  }

  String tokenProgramUser() {
    return signer.getToken(email(), "PROGRAM-TEST-CA.READ");
  }

  String tokenAdminUserWrongProgram() {
    return signer.getToken(email(), "PROGRAM-TEST-DK.WRITE");
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
    return new CountryEntity().setName(name).setId(UUID.randomUUID());
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
      setCommitmentDonors(30000).setProgramCountries(countries(p, "CA")).setCreatedAt(created).setDescription("Fake 3")
      .setGenomicDonors(30).
        setMembershipType(MembershipType.FULL).setProgramCancers(Set.of()).setProgramPrimarySites(Set.of()).
        setWebsite("http://org.com");
  }

  Program program() {
    return Program.newBuilder().
      setMembershipType(MembershipTypeValue.newBuilder().setValue(MembershipType.FULL).build()).
      setCommitmentDonors(Int32Value.of(1000)).
      setSubmittedDonors(Int32Value.of(0)).
      setGenomicDonors(Int32Value.of(0)).
      setName(programName()).
      setShortName(programName()).
      setWebsite(website()).
      build();
  }

  String dccAdminPermission() {
    return "PROGRAMSERVICE.WRITE";
  }

  String email() {
    return "x@test.com";
  }

  StringValue userId() {
    return StringValue.of(email());
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
    return UpdateUserRequest.newBuilder().setUserEmail(userId()).setShortName(programName()).build();
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

  JoinProgramInviteEntity invite1() {
    return new JoinProgramInviteEntity().setUserEmail(userId().getValue());
  }

  JoinProgramInviteEntity invite2() {
    return new JoinProgramInviteEntity().setUserEmail("y@z.com");
  }

  RemoveUserRequest removeUserRequest() {
    return RemoveUserRequest.newBuilder().setProgramShortName(programName()).setUserEmail(userId()).build();
  }

}

@AllArgsConstructor
class Signer {
  private final Key privateKey;

  public String getToken(String email, String... permissions) {
    return getToken(email, false, false, permissions);
  }

  public String getToken(String email, boolean isExpired, boolean isEgoAdmin, String... permissions) {
    val issued = Date.from(Instant.now());
    // our jwt expires in one hour -- our test should take much less than that to run
    Date expires;
    if (isExpired) {
      expires = Date.from(Instant.now().minusSeconds(1));
    } else {
      expires = Date.from(Instant.now().plusSeconds(3600));
    }
    val context = new Context();
    val u = getUser(isEgoAdmin);
    u.setCreatedAt(issued.toString());
    u.setEmail(email);
    context.setUser(u);
    context.setScope(permissions);

    return Jwts.builder()
      .setIssuedAt(issued)
      .setIssuer("ego")
      .setExpiration(expires)
      .claim("context", context)
      .signWith(SignatureAlgorithm.RS256, privateKey)
      .compact();
  }

  public Context.User getUser(boolean isAdmin) {
    val u = new Context.User();

    u.setName("Test User");
    u.setFirstName("Test");
    u.setLastName("Test");
    if (isAdmin) {
      u.setType("Admin");
    } else {
      u.setType("USER");
    }
    u.setGroups(new String[0]);

    return u;

  }
}

// Helper classes

/***
 * Utility class for tests of a given RemoteProcedureCall endpoint.
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

  /***
   * Checks whether the test returned an RuntimeStatusException with the given status; prints a message to stderr if so.
   * @param testName
   * @param status
   * @return true if the test returned a RuntimeStatusException with the given status, false otherwise.
   */
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

  /***
   * Checks whether the test threw an exception; prints a message to stderr if so
   * @param testName
   * @return false if an exception was thrown, otherwise true.
   */
  boolean threwNoExceptions(String testName) {
    if (exception != null) {
      System.err.printf("In test %s, call to %s threw %s\n", testName, rpcName, exception.getMessage());
      return false;
    }
    return true;
  }
}

@AllArgsConstructor
  /***
   * A class for running a series of endpoint tests, and testing the outcomes against expected results.
   */
class AuthorizationTest implements Runnable {
  String testName;
  List<EndpointTest> tests;

  public void run() {
    tests.stream().forEach(t -> t.run());
  }

  /***
   * Check whether all the tests threw no exceptions.
   * @return false if one of the tests threw an exception, true otherwise.
   */
  boolean threwNoExceptions() {
    return threwNoExceptions(0, tests.size());
  }

  /***
   * Check whether all the tests from <start> threw no exceptions.
   * @param start The number of the test to start with.
   * @return false if one of the tests threw an exception, true otherwise.
   */
  boolean threwNoExceptions(Integer start) {
    return threwNoExceptions(start, tests.size());
  }

  /***
   * Check whether all tests starting with start, and less than stop threw no exceptions.
   * @param start The number of the test to start with.
   * @param stop  The number of the test to stop before.
   * @return
   */
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

  /***
   * Check whether all of the tests threw a RuntimeStatusException with the expected Status
   * @param expectedStatus The status of the RunTimeStatusException we expect.
   * @return false if one of the tests threw
   */
  boolean threwStatusException(Status expectedStatus) {
    return threwStatusException(expectedStatus, 0, tests.size());
  }

  /***
   * Check whether all of the tests  starting from start threw a RuntimeStatusException with the expected Status
   * @param expectedStatus The status of the RunTimeStatusException we expect.
   * @param start The number of the test to start from
   * @return false if one of the tests threw
   */
  boolean threwStatusException(Status expectedStatus, Integer start) {
    return threwStatusException(expectedStatus, start, tests.size());
  }

  /***
   * Check whether all of the tests between start and stop threw a RuntimeStatusException with the expected status.
   * @param expectedStatus The status of the RunTimeStatusException we expect.
   * @return false if one of the tests threw
   */
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

