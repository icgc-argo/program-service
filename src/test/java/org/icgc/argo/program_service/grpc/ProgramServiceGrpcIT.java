/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

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
import org.icgc.argo.program_service.model.join.ProgramInstitutionId;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.repositories.InstitutionRepository;
import org.icgc.argo.program_service.repositories.ProgramInstitutionRepository;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.utils.EntityGenerator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class ProgramServiceGrpcIT {
  @ClassRule
  public static GenericContainer mailhogContainer =
      new FixedHostPortGenericContainer("mailhog/mailhog:v1.0.0")
          .withFixedExposedPort(10200, 8025) // http port used in waitForHttp
          .withFixedExposedPort(10300, 1025) // mail port used by application
          .waitingFor(
              Wait.forHttp("/")); // Define wait condition during startup, checks lowest host port

  @Autowired private ProgramServiceImpl programServiceImpl;

  @Autowired private InstitutionRepository institutionRepository;

  @Autowired private ProgramInstitutionRepository programInstitutionRepository;

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
  // There is 1 region in db.
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

    // get with new short name and find the program
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

  @Test
  public void updateProgramAddNewInstitution() {
    // setup program
    val name = stringValue(randomProgramName());
    val initalProgram =
        Program.newBuilder()
            .setShortName(name)
            .setMembershipType(membershipTypeValue(ASSOCIATE))
            .setWebsite(stringValue("http://site.org"))
            .addInstitutions("Ontario Institute for Cancer Research")
            .setName(stringValue(RandomString.make(15)))
            .setCommitmentDonors(int32Value(234))
            .addCountries("Canada")
            .setSubmittedDonors(int32Value(244))
            .setGenomicDonors(int32Value(333))
            .setDescription(stringValue("nothing"))
            .addCancerTypes("Blood cancer")
            .addPrimarySites("Blood");
    val programEntity = entityGenerator.createProgramEntity(initalProgram.build());

    // check new institution not in institutions db
    val newInstitution = "Test-Institution";
    assertFalse(institutionRepository.getInstitutionByName(newInstitution).isPresent());

    // Update program - add new institution
    val updatedProgram = initalProgram.addInstitutions(newInstitution).build();
    val updateProgramRequest = UpdateProgramRequest.newBuilder().setProgram(updatedProgram).build();
    val response = stub.updateProgram(updateProgramRequest);

    // check new institution has been added to institution repo
    val newInstitutionEntity = institutionRepository.getInstitutionByName(newInstitution).get();
    assertEquals(newInstitution, newInstitutionEntity.getName());

    // check programInstitution relation between new institution and existing program
    val programInstitutionId =
        ProgramInstitutionId.builder()
            .institutionId(newInstitutionEntity.getId())
            .programId(programEntity.getId())
            .build();
    val newProgramInstitution = programInstitutionRepository.findById(programInstitutionId).get();
    assertEquals(newInstitution, newProgramInstitution.getInstitution().getName());
    assertEquals(name.getValue(), newProgramInstitution.getProgram().getShortName());
  }
}
