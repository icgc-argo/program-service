package org.icgc.argo.program_service.grpc;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.icgc.argo.program_service.UtilsTest.*;
import static org.icgc.argo.program_service.proto.MembershipType.ASSOCIATE;
import static org.junit.jupiter.api.Assertions.*;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.bytebuddy.utility.RandomString;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.repositories.InstitutionRepository;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.utils.EntityGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class ProgramServiceGrpcIT {

  @Autowired private ProgramServiceImpl programServiceImpl;

  @Autowired private InstitutionRepository institutionRepository;

  @Autowired private EntityGenerator entityGenerator;

  @Autowired private EgoClient egoClient;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private String serverName;

  private Channel channel;

  private ProgramServiceGrpc.ProgramServiceBlockingStub stub;

  // There are 36 cancers in db.
  private final int CANCER_COUNT = 36;
  // There are 23 primary sites in db.
  private final int PRIMARY_SITE_COUNT = 23;
  // There are 6 regions in db.
  private final int REGION_COUNT = 1;
  // There are at least 435 institutions in db.
  private final int LEAST_INSTITUTION_COUNT = 435;
  // There are 245 countries in db.
  private final int COUNTRY_COUNT = 245;

  private final String INSTITUTION_1 = "XZY-GROUP";
  private final String INSTITUTION_2 = "Example Lab";
  private final String EXISTING_INSTITUTION_1 = "Aarhus University";
  private final String EXISTING_INSTITUTION_2 = "Biobyte solutions GmbH";
  private final String NEW_USER_EMAIL = randomAlphabetic(15) + "@gmail.com";

  @Before
  public void before() throws IOException {

    // setUpInProcessGrpc
    // Generate a unique in-process server name.
    serverName = InProcessServerBuilder.generateName();
    // Create a client channel and register for automatic graceful shutdown.
    channel =
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(programServiceImpl)
            .build()
            .start());

    stub = ProgramServiceGrpc.newBlockingStub(channel);
  }

  @Test
  public void createProgramAndGet() {
    val shortName = stringValue(randomProgramName());
    val program =
        Program.newBuilder()
            .setShortName(shortName)
            .setMembershipType(membershipTypeValue(ASSOCIATE))
            .setWebsite(stringValue("http://site.org"))
            .addInstitutions("Ontario Institute for Cancer Research")
            .addRegions("Canada")
            .setName(stringValue(RandomString.make(15)))
            .setCommitmentDonors(int32Value(234))
            .addCountries("Canada")
            .setSubmittedDonors(int32Value(244))
            .setGenomicDonors(int32Value(333))
            .setDescription(stringValue("nothing"))
            .addCancerTypes("Blood cancer")
            .addPrimarySites("Blood");

    val admins =
        List.of(
            User.newBuilder()
                .setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN))
                .setEmail(StringValue.of("x@test.com"))
                .setFirstName(StringValue.of("Test"))
                .setLastName(StringValue.of("User"))
                .build());

    val createProgramRequest =
        CreateProgramRequest.newBuilder().setProgram(program).addAllAdmins(admins).build();
    val response = stub.createProgram(createProgramRequest);
    assertFalse(isEmpty(response.getCreatedAt()));

    val getProgramRequest = GetProgramRequest.newBuilder().setShortName(shortName).build();
    val getResponse = stub.getProgram(getProgramRequest);
    assertEquals(
        shortName.getValue(), getResponse.getProgram().getProgram().getShortName().getValue());
  }

  boolean isEmpty(Object o) {
    return o == null || o.toString().length() == 0;
  }

  @Test
  public void activateProgramAndGet() {
    val oldShortName = stringValue("PACA-AU");
    val newShortName = stringValue(randomProgramName());

    val admins =
        List.of(
            User.newBuilder()
                .setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN))
                .setEmail(StringValue.of("x@test.com"))
                .setFirstName(StringValue.of("Test"))
                .setLastName(StringValue.of("User"))
                .build());

    val activateProgramRequest =
        ActivateProgramRequest.newBuilder()
            .setOriginalShortName(oldShortName)
            .setUpdatedShortName(newShortName)
            .addAllAdmins(admins)
            .build();
    // .setProgram(program).addAllAdmins(admins).build();
    val response = stub.activateProgram(activateProgramRequest);
    assertTrue(response.getProgram().hasProgram());
    assertEquals(response.getProgram().getProgram().getShortName(), newShortName);

    // get old short name finds nothing (error)
    val getProgramRequestNewName =
        GetProgramRequest.newBuilder().setShortName(newShortName).build();
    val getResponseNewName = stub.getProgram(getProgramRequestNewName);
    assertTrue(getResponseNewName.getProgram().hasProgram());
  }

  @Test
  public void joinAndLeaveProgram() {
    val name = stringValue(randomProgramName());
    val program =
        Program.newBuilder()
            .setShortName(name)
            .setMembershipType(membershipTypeValue(ASSOCIATE))
            .setWebsite(stringValue("http://site.org"))
            .addInstitutions("Ontario Institute for Cancer Research")
            .addRegions("Canada")
            .setName(stringValue(RandomString.make(15)))
            .setCommitmentDonors(int32Value(234))
            .addCountries("Canada")
            .setSubmittedDonors(int32Value(244))
            .setGenomicDonors(int32Value(333))
            .setDescription(stringValue("nothing"))
            .addCancerTypes("Blood cancer")
            .addPrimarySites("Blood");

    val admins =
        List.of(
            User.newBuilder()
                .setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN))
                .setEmail(StringValue.of("x@test.com"))
                .setFirstName(StringValue.of("Test"))
                .setLastName(StringValue.of("User"))
                .build());

    val createProgramRequest =
        CreateProgramRequest.newBuilder().setProgram(program).addAllAdmins(admins).build();
    val response = stub.createProgram(createProgramRequest);

    val inviteUserRequest =
        InviteUserRequest.newBuilder()
            .setFirstName(stringValue("First"))
            .setLastName(stringValue("Last"))
            .setEmail(stringValue("user@example.com"))
            .setRole(userRoleValue(UserRole.ADMIN))
            .setProgramShortName(name)
            .build();
    val inviteUserResponse = stub.inviteUser(inviteUserRequest);
    assertFalse(isEmpty(inviteUserResponse.getInviteId().getValue()));
  }

  @Test
  public void invite_new_user_user_gets_added() {
    val shortname = randomProgramName();
    entityGenerator.setUpProgramEntity(shortname);
    assertFalse(egoClient.getUser(NEW_USER_EMAIL).isPresent());

    val request =
        InviteUserRequest.newBuilder()
            .setEmail(CommonConverter.INSTANCE.boxString(NEW_USER_EMAIL))
            .setFirstName(CommonConverter.INSTANCE.boxString("Hermione"))
            .setLastName(CommonConverter.INSTANCE.boxString("Granger"))
            .setProgramShortName(CommonConverter.INSTANCE.boxString(shortname))
            .build();
    val response = stub.inviteUser(request);

    assertNotNull(response.getInviteId());
    assertTrue(egoClient.getUser(NEW_USER_EMAIL).isPresent());
    val user = egoClient.getUser(NEW_USER_EMAIL).get();

    assertEquals("Hermione", egoClient.getUser(NEW_USER_EMAIL).get().getFirstName());
    assertEquals("Granger", egoClient.getUser(NEW_USER_EMAIL).get().getLastName());

    egoClient.deleteUserById(user.getId());
    assertFalse(egoClient.getUser(NEW_USER_EMAIL).isPresent());
  }

  String randomProgramName() {
    return randomAlphabetic(7).toUpperCase() + "-CA";
  }

  @Test
  public void list_cancers() {
    val response = stub.listCancers(Empty.getDefaultInstance());
    assertEquals(CANCER_COUNT, response.getCancersList().size());
  }

  @Test
  public void list_primary_sites() {
    val response = stub.listPrimarySites(Empty.getDefaultInstance());
    assertEquals(PRIMARY_SITE_COUNT, response.getPrimarySitesList().size());
  }

  @Test
  public void list_institutions() {
    val response = stub.listInstitutions(Empty.getDefaultInstance());
    // Original institution list contains 435 institutions, new institutions may be added
    assertTrue(response.getInstitutionsList().size() >= LEAST_INSTITUTION_COUNT);
  }

  @Test
  public void list_regions() {
    val response = stub.listRegions(Empty.getDefaultInstance());
    assertEquals(REGION_COUNT, response.getRegionsList().size());
  }

  @Test
  public void list_countries() {
    val response = stub.listCountries(Empty.getDefaultInstance());
    assertEquals(COUNTRY_COUNT, response.getCountriesList().size());
  }

  @Test
  public void add_institutions_empty_name_fail() {
    val request =
        AddInstitutionsRequest.newBuilder()
            .addNames(CommonConverter.INSTANCE.boxString(""))
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_1))
            .build();
    assertThrows(StatusRuntimeException.class, () -> stub.addInstitutions(request));
  }

  @Test
  public void add_institution_duplicate_fail() {
    val request =
        AddInstitutionsRequest.newBuilder()
            .addNames(CommonConverter.INSTANCE.boxString(EXISTING_INSTITUTION_1))
            .addNames(CommonConverter.INSTANCE.boxString(EXISTING_INSTITUTION_2))
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_1))
            .build();
    assertThrows(StatusRuntimeException.class, () -> stub.addInstitutions(request));
  }

  @Test
  public void add_unique_new_institution_success() {
    val request =
        AddInstitutionsRequest.newBuilder()
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_1))
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_2))
            .build();

    assertFalse(institutionRepository.getInstitutionByName(INSTITUTION_1).isPresent());
    assertFalse(institutionRepository.getInstitutionByName(INSTITUTION_2).isPresent());

    val response = stub.addInstitutions(request);
    val names =
        response.getInstitutionsList().stream()
            .map(Institution::getName)
            .map(name -> CommonConverter.INSTANCE.unboxStringValue(name))
            .collect(Collectors.toList());

    assertTrue(institutionRepository.getInstitutionByName(INSTITUTION_1).isPresent());
    assertTrue(institutionRepository.getInstitutionByName(INSTITUTION_2).isPresent());
    assertEquals(2, response.getInstitutionsList().size());
    assertTrue(names.contains(INSTITUTION_1));
    assertTrue(names.contains(INSTITUTION_2));
  }

  @Test
  public void listPrograms() {
    val programEntity_1 = entityGenerator.setUpProgramEntity(randomProgramName());
    val programEntity_2 = entityGenerator.setUpProgramEntity(randomProgramName());
    val programEntity_3 = entityGenerator.setUpProgramEntity(randomProgramName());

    val response = stub.listPrograms(Empty.getDefaultInstance());

    assertTrue(response.getProgramsCount() == 3);
    val nameList =
        response.getProgramsList().stream()
            .map(ProgramDetails::getProgram)
            .map(Program::getShortName)
            .map(CommonConverter.INSTANCE::unboxStringValue)
            .collect(toUnmodifiableList());
    assertTrue(nameList.contains(programEntity_1.getShortName()));
    assertTrue(nameList.contains(programEntity_2.getShortName()));
    assertTrue(nameList.contains(programEntity_3.getShortName()));
  }
}
