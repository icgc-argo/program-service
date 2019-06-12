/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.icgc.argo.program_service.grpc;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.EgoService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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
  public void testList() {
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
    assertThat(programList.stream().map(ProgramDetails::getProgram)).contains(p1, p2);
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
        .setName(StringValue.of(name))
        .setShortName(StringValue.of(shortName))
        .setDescription(StringValue.of(description))
        .setMembershipType(MembershipTypeValue.newBuilder()
            .setValue( MembershipType.valueOf(membershipType))
            .build())
        .setCommitmentDonors(Int32Value.of(commitmentDonors))
        .setSubmittedDonors(Int32Value.of(submittedDonors))
        .setGenomicDonors(Int32Value.of(genomicDonors))
        .setWebsite(StringValue.of(website))
        .setCountries(StringValue.of("Canada"))
        .addCancerTypes(CancerType.Brain_cancer)
        .addCancerTypes(CancerType.Blood_cancer)
        .addPrimarySites(PrimarySite.Brain)
        .addPrimarySites(PrimarySite.Blood)
      .build();
    return p;
  }

  public void createProgram(Program p) {
    val details = CreateProgramRequest
      .newBuilder()
      .setProgram(p)
      .addAdminEmails("test@gmailcom")
      .build();
    val resultObserver = new TestObserver<CreateProgramResponse>();
    try {
      programService.createProgram(details, resultObserver);
    } catch(EgoService.ConflictException e) {

    }
    // assertTrue(resultObserver.completed);
    //assertNull(resultObserver.thrown);
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

