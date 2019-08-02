package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Empty;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.proto.AddInstitutionsRequest;
import org.icgc.argo.program_service.proto.Institution;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc;
import org.icgc.argo.program_service.repositories.InstitutionRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@ActiveProfiles("test")
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class ProgramServiceGrpcTest {

  @Autowired
  private ProgramServiceImpl programServiceImpl;

  @Autowired
  private InstitutionRepository institutionRepository;

  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private String serverName;

  private Channel channel;

  private ProgramServiceGrpc.ProgramServiceBlockingStub stub;

  // There are 35 cancers in db.
  private final int CANCER_COUNT = 35;
  // There are 22 primary sites in db.
  private final int PRIMARY_SITE_COUNT = 22;
  // There are 6 regions in db.
  private final int REGION_COUNT = 6;
  // There are at least 435 institutions in db.
  private final int LEAST_INSTITUTION_COUNT = 435;
  // There are 245 countries in db.
  private final int COUNTRY_COUNT = 245;

  private final String INSTITUTION_1 = "XZY-GROUP";
  private final String INSTITUTION_2 = "Example Lab";
  private final String EXISTING_INSTITUTION_1 = "Aarhus University";
  private final String EXISTING_INSTITUTION_2 = "Biobyte solutions GmbH";

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
  public void list_cancers(){
    val response = stub.listCancers(Empty.getDefaultInstance());
    assertThat(response.getCancersList().size()).isEqualTo(CANCER_COUNT);
  }

  @Test
  public void list_primary_sites(){
    val response = stub.listPrimarySites(Empty.getDefaultInstance());
    assertThat(response.getPrimarySitesList().size()).isEqualTo(PRIMARY_SITE_COUNT);
  }

  @Test
  public void list_institutions(){
    val response = stub.listInstitutions(Empty.getDefaultInstance());
    // Original institution list contains 435 institutions, new institutions may be added
    assertThat(response.getInstitutionsList().size()).isGreaterThanOrEqualTo(LEAST_INSTITUTION_COUNT);
  }

  @Test
  public void list_regions(){
    val response = stub.listRegions(Empty.getDefaultInstance());
    assertThat(response.getRegionsList().size()).isEqualTo(REGION_COUNT);
  }

  @Test
  public void list_countries(){
    val response = stub.listCountries(Empty.getDefaultInstance());
    assertThat(response.getCountriesList().size()).isEqualTo(COUNTRY_COUNT);
  }

  @Test
  public void add_institutions_empty_name_fail(){
    val request = AddInstitutionsRequest.newBuilder()
            .addNames(CommonConverter.INSTANCE.boxString(""))
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_1))
            .build();
    assertThrows(StatusRuntimeException.class, () -> stub.addInstitutions(request));
  }

  @Test
  public void add_institution_duplicate_fail(){
    val request = AddInstitutionsRequest.newBuilder()
            .addNames(CommonConverter.INSTANCE.boxString(EXISTING_INSTITUTION_1))
            .addNames(CommonConverter.INSTANCE.boxString(EXISTING_INSTITUTION_2))
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_1))
            .build();
    assertThrows(StatusRuntimeException.class, () -> stub.addInstitutions(request));
  }

  @Test
  public void add_unique_new_institution_success(){
    val request = AddInstitutionsRequest.newBuilder()
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_1))
            .addNames(CommonConverter.INSTANCE.boxString(INSTITUTION_2))
            .build();

    assertThat(institutionRepository.getInstitutionByName(INSTITUTION_1).isPresent()).isFalse();
    assertThat(institutionRepository.getInstitutionByName(INSTITUTION_2).isPresent()).isFalse();

    val response = stub.addInstitutions(request);
    val names = response.getInstitutionsList()
            .stream()
            .map(Institution::getName)
            .map(name -> CommonConverter.INSTANCE.unboxStringValue(name))
            .collect(Collectors.toList());

    assertThat(institutionRepository.getInstitutionByName(INSTITUTION_1).isPresent()).isTrue();
    assertThat(institutionRepository.getInstitutionByName(INSTITUTION_2).isPresent()).isTrue();
    assertThat(response.getInstitutionsList().size()).isEqualTo(2);
    assertThat(names.contains(INSTITUTION_1)).isTrue();
    assertThat(names.contains(INSTITUTION_2)).isTrue();
  }

}
