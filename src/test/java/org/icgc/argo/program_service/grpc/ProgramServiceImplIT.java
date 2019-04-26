package org.icgc.argo.program_service.grpc;

import com.google.common.collect.Streams;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.shaded.com.google.common.collect.Sets;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;
import static org.assertj.core.api.Assertions.assertThat;

// TODO: program service is already running at the phase of pre-integration-test, use the existing running 50051 port
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({ "test", "default" })
public class ProgramServiceImplIT {

  @Autowired
  private ProgramServiceImpl programService;

  @Test
  public void test_list() {
    // case 1: empty list
    val request = Empty.getDefaultInstance();
    val programsObserver = new TestObserver<ListProgramsResponse>();
    programService.listPrograms(request, programsObserver);
    assertTrue(programsObserver.completed);
    assertNull(programsObserver.thrown);

    val p1 = buildProgram("Alice Bob and Charlie's Test Program Name",
      "ABC",
      "A test project. Fix this description later.",
      "FULL",
      100000000,
      1,
      3,
      "http://abc-project.test"
    );
    createProgram(p1);

    val p2 = buildProgram("Xander's Yearly Zoological Experimentations",
      "XYZ",
      "This needs a better description.",
      "ASSOCIATE",
      500000000,
      25,
      55,
      "http://xyz.org"
    );

    createProgram(p2);

    programService.listPrograms(request, programsObserver);
    assertTrue(programsObserver.completed);
    assertNull(programsObserver.thrown);

    val programList = programsObserver.result.getProgramsList();

    assertEquals("Two programs", 2, programList.size());

    val expectedPrograms = Sets.newHashSet();
    expectedPrograms.add(p1);
    expectedPrograms.add(p2);
    val actualPrograms =
      programList.stream()
      .map(GetProgramResponse::getProgram)
      .collect(Collectors.toUnmodifiableSet());

    assertThat(actualPrograms).isEqualTo(expectedPrograms);
  }

  public Program buildProgram(String name,
     String shortName,
     String description,
     String membershipType,
     int commitmentDonors,
     int submittedDonors,
     int genomicDonors,
     String website
  ) {
    val p = Program
      .newBuilder()
      .setName(name)
      .setShortName(shortName)
      .setDescription(description)
      .setMembershipType(MembershipType.valueOf(membershipType))
      .setCommitmentDonors(commitmentDonors)
      .setSubmittedDonors(submittedDonors)
      .setGenomicDonors(genomicDonors)
      .setWebsite(website)
      .setCountries("Canada")
      .build();
    return p;
  }

  public void createProgram(Program p) {
    val details = CreateProgramRequest
      .newBuilder()
      .setProgram(p)
      .build();
    val resultObserver = new TestObserver<CreateProgramResponse>();
    programService.createProgram(details, resultObserver);
    assertTrue(resultObserver.completed);
    assertNull(resultObserver.thrown);
  }
}

class TestObserver<T> implements StreamObserver<T> {
  public T result;
  public Throwable thrown;
  public boolean completed = false;

  @Override public void onNext(T value) {
    result = value;
  }

  @Override public void onError(Throwable throwable) {
    thrown = throwable;
  }

  @Override public void onCompleted() {
    completed = true;
  }
}

