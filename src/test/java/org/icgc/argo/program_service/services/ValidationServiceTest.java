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

package org.icgc.argo.program_service.services;

import static java.lang.String.format;
import static org.icgc.argo.program_service.utils.CollectionUtils.join;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import java.util.List;
import lombok.val;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.CountryEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.RegionEntity;
import org.icgc.argo.program_service.properties.ValidationProperties;
import org.icgc.argo.program_service.proto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {
  private final int _init = init();
  ProgramService programService;
  ValidationService validationService;

  int init() {
    this.programService = mock(ProgramService.class);
    when(programService.listCancers())
        .thenReturn(cancers("Blood cancer", "Brain cancer", "Renal cancer"));
    when(programService.listPrimarySites()).thenReturn(primarySites("Blood", "Brain", "Liver"));
    when(programService.listCountries()).thenReturn(countries("Canada", "Denmark"));
    when(programService.listRegions()).thenReturn(regions("North America", "Europe"));

    this.validationService =
        new ValidationService(programService, new ValidationProperties().factory());

    return 1;
  }

  private List<CancerEntity> cancers(String... names) {
    return mapToList(
        names,
        s -> {
          val c = new CancerEntity();
          c.setName(s);
          return c;
        });
  }

  private List<PrimarySiteEntity> primarySites(String... names) {
    return mapToList(
        names,
        s -> {
          val c = new PrimarySiteEntity();
          c.setName(s);
          return c;
        });
  }

  private List<CountryEntity> countries(String... names) {
    return mapToList(
        names,
        s -> {
          val c = new CountryEntity();
          c.setName(s);
          return c;
        });
  }

  private List<RegionEntity> regions(String... names) {
    return mapToList(
        names,
        s -> {
          val c = new RegionEntity();
          c.setName(s);
          return c;
        });
  }

  private List<User> admins(String... emails) {
    return mapToList(
        emails,
        email -> {
          return User.newBuilder()
              .setEmail(StringValue.of(email))
              .setFirstName(StringValue.of("Not"))
              .setLastName(StringValue.of("Important"))
              .setRole(UserRoleValue.newBuilder().setValue(UserRole.ADMIN).build())
              .build();
        });
  }

  private Program goodProgram() {
    return Program.newBuilder()
        .setCommitmentDonors(Int32Value.of(1000))
        .setGenomicDonors(Int32Value.of(0))
        .setMembershipType(
            MembershipTypeValue.newBuilder().setValueValue(MembershipType.ASSOCIATE_VALUE).build())
        .setName(StringValue.of("A wonderful research program"))
        .setShortName(StringValue.of("TEST-CA"))
        .setWebsite(StringValue.of("http://www.site.com"))
        .setSubmittedDonors(Int32Value.of(0))
        .addAllCancerTypes(List.of("Blood cancer", "Renal cancer"))
        .addAllPrimarySites(List.of("Blood", "Liver"))
        .addAllCountries(List.of("Canada", "Denmark"))
        .addAllRegions(List.of("North America", "Europe"))
        .addAllInstitutions(List.of("OICR", "New Institute of Novel Innovations"))
        .build();
  }

  void createProgramExpectingErrorMessage(CreateProgramRequest request, String expectedMessage) {
    String result = "";
    try {
      val errors = validationService.validateCreateProgramRequest(request);
      result = join(errors, ", ");
    } catch (Exception ex) {
      fail(format("Threw exception %s", ex.getMessage()));
    }
    assertEquals("Incorrect error message", expectedMessage, result);
  }

  @Test
  void createProgramEmptyRequest() {
    val request = CreateProgramRequest.newBuilder().build();
    createProgramExpectingErrorMessage(
        request,
        "No program in the CreateProgram request, A program must have at least one administrator");
  }

  @Test
  void createProgramNoAdmins() {
    val request =
        CreateProgramRequest.newBuilder().setProgram(goodProgram()).addAllAdmins(admins()).build();

    createProgramExpectingErrorMessage(request, "A program must have at least one administrator");
  }

  @Test
  void createProgramInvalidEmail() {
    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(goodProgram())
            .addAllAdmins(admins("invalid"))
            .build();
    createProgramExpectingErrorMessage(
        request, "Invalid email address 'invalid' for admin 'Not Important'");
  }

  @Test
  void createProgramEmptyProgram() {
    val program = Program.newBuilder().build();

    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(program)
            .addAllAdmins(admins("valid@test.com"))
            .build();

    createProgramExpectingErrorMessage(
        request,
        "commitmentDonors must not be null, "
            + "genomicDonors must not be null, membershipType must not be null, name must not be null, "
            + "shortName must not be null, submittedDonors must not be null, website must not be null, "
            + "Must include at least one cancerType, Must include at least one primarySite, "
            + "Must include at least one region, Must include at least one country");
  }

  @Test
  void createProgramInvalidShortName() {
    val program = goodProgram().toBuilder().setShortName(StringValue.of("invalid")).build();

    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(program)
            .addAllAdmins(admins("valid@test.com"))
            .build();

    createProgramExpectingErrorMessage(request, "shortName is invalid");
  }

  @Test
  void createProgramInvalidWebSite() {
    val program = goodProgram().toBuilder().setWebsite(StringValue.of("bad.com")).build();

    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(program)
            .addAllAdmins(admins("valid@test.com"))
            .build();

    createProgramExpectingErrorMessage(request, "website must be a valid URL");
  }

  @Test
  void createProgramBadCancerTypes() {
    val program =
        goodProgram().toBuilder().addAllCancerTypes(List.of("Not cancer", "Bad name")).build();

    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(program)
            .addAllAdmins(admins("valid@test.com"))
            .build();
    createProgramExpectingErrorMessage(
        request, "Invalid cancerType 'Bad name', Invalid cancerType 'Not cancer'");
  }

  @Test
  void createProgramBadPrimarySites() {
    val program =
        goodProgram()
            .toBuilder()
            .clearPrimarySites()
            .addAllPrimarySites(List.of("Brain", "Blood", "Invalid", "Liver", "Onions"))
            .build();

    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(program)
            .addAllAdmins(admins("valid@test.com"))
            .build();
    createProgramExpectingErrorMessage(
        request, "Invalid primarySite 'Invalid', Invalid primarySite 'Onions'");
  }

  @Test
  void createProgramBadCountries() {
    val program =
        goodProgram()
            .toBuilder()
            .clearCountries()
            .addAllCountries(List.of("Canada", "New Freedonia", "Denmark"))
            .build();

    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(program)
            .addAllAdmins(admins("valid@test.com"))
            .build();
    createProgramExpectingErrorMessage(request, "Invalid country 'New Freedonia'");
  }

  @Test
  void createProgramBadRegions() {
    val program =
        goodProgram().toBuilder().addAllRegions(List.of("North America", "Europa")).build();

    val request =
        CreateProgramRequest.newBuilder()
            .setProgram(program)
            .addAllAdmins(admins("valid@test.com"))
            .build();
    createProgramExpectingErrorMessage(request, "Invalid region 'Europa'");
  }
}
