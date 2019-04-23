package org.icgc.argo.program_service.grpc;

import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProgramServiceImplTest {
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

    createProgram("Alice Bob and Charlie's Test Program Name",
      "ABC",
      "A test project. Fix this description later.",
      "FULL",
      100000000,
      1,
      3,
      "http://abc-project.test"
    );

    createProgram("Xander's Yearly Zoological Experimentations",
      "XYZ",
      "This needs a better description.",
      "ASSOCIATE",
      500000000,
      25,
      55,
      "http://xyz.org"
    );

    programService.list(request, programsObserver);
    assertTrue(programsObserver.completed);
    assertNull(programsObserver.thrown);
    System.err.printf("Got programs -->%s<--\n",programsObserver.programs.toString());
  }
  
  public void createProgram(String name,
     String shortName,
     String description,
     String membershipType,
     int commitmentDonors,
     int submittedDonors,
     int genomicDonors,
     String website
  ) {
    val p1= Program
      .newBuilder()
      .setName(name)
      .setShortName(shortName)
      .setDescription(description)
      .setMembershipType(MembershipType.valueOf(membershipType))
      .setCommitmentDonors(commitmentDonors)
      .setSubmittedDonors(submittedDonors)
      .setGenomicDonors(genomicDonors)
      .setWebsite(website)
      //.setId(UUID.randomUUID().toString())
      .setId("23d77265-b613-4726-aad5-c85636a6b7fb")
      .setDateCreated(Date.newBuilder().build())
      .build();

    // case2: single program
    val d1 = ProgramDetails
      .newBuilder()
      .setProgram(p1)
      .build();
    val resultObserver = new TestObserver<ProgramDetails>();
    programService.create(d1, resultObserver);

  }
}

class TestObserver<T> implements StreamObserver<T> {
  public T programs;
  public Throwable thrown;
  public boolean completed = false;

  @Override public void onNext(T programCollection) {
    programs = programCollection;
  }

  @Override public void onError(Throwable throwable) {
    thrown = throwable;
  }

  @Override public void onCompleted() {
    completed = true;
  }
}
