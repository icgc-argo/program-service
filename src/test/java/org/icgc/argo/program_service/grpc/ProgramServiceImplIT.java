package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Timestamp;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.*;

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
    val programsObserver = new TestObserver<ProgramCollection>();
    programService.list(request, programsObserver);
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

    programService.list(request, programsObserver);
    assertTrue(programsObserver.completed);
    assertNull(programsObserver.thrown);

    val programList = programsObserver.result.getProgramsList();
    System.err.printf("Got result -->%s<--\n",programList.toString());

    assertEquals("Two programs", 2, programList.size());

    val t1 = Sets.newTreeSet( new ProgramComparator());
    val t2 = Sets.newTreeSet( new ProgramComparator());

    t1.addAll(programList);
    t2.add(p1);
    t2.add(p2);
    assertEquals("Same programs except for id and date", t1,t2);
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
    Instant instant = Instant.now();
    val timestamp = Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();

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
      // gets over-written, but we still need to set it.
      .setId(UUID.randomUUID().toString())
      .setCreatedAt(timestamp)
      .build();
    return p;
  }

  public void createProgram(Program p) {
    val details = ProgramDetails.newBuilder().setProgram(p).build();
    val resultObserver = new TestObserver<ProgramDetails>();
    programService.create(details, resultObserver);
    assertTrue(resultObserver.completed);
    assertNull(resultObserver.thrown);
  }
}

// compare all the fields of our Program except for date and id
class ProgramComparator extends PartialComparison<Program> {
  @Override
  List<Comparable> contents(Program object) {
    return List.of(
      object.getName(),
      object.getShortName(),
      object.getDescription(),
      object.getCommitmentDonors(),
      object.getSubmittedDonors(),
      object.getGenomicDonors(),
      object.getWebsite()
    );
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

