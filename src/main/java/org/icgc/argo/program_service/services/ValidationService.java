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

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.ProgramConverterImpl;
import org.icgc.argo.program_service.model.entity.*;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.ConstraintViolation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Email;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToSet;

@Service
@Validated
@Slf4j
public class ValidationService {

  /**
   * Dependencies
   */
  private final ProgramService programService;
  private final ValidatorFactory validatorFactory;

  @Autowired
  public ValidationService(
    @NonNull ProgramService programService,
    @NonNull ValidatorFactory validatorFactory) {
    this.programService = programService;
    this.validatorFactory = validatorFactory;
  }

  public List<String> validateCreateProgramRequest(CreateProgramRequest request) {
    List<String> errors = new ArrayList<>();

    if (!request.hasProgram()) {
      errors.add("No program in the CreateProgram request");
    } else if (request.getProgram() == null) {
      errors.add("Program may not be null");
    } else {
      errors.addAll(validateProgram(request.getProgram()));
    }

    if (request.getAdminsCount() == 0) {
      errors.add("A program must have at least one administrator");
    } else if (request.getAdminsList() == null) {
      errors.add("The administration list may not be null");
    } else {
      for (val user : request.getAdminsList()) {
        errors.addAll(validateUser(user));
      }
    }

    return errors;
  }

  private List<String> validateUser(User user) {
    val check = new EmailCheck(user.getEmail().getValue());

    val constraints = validatorFactory.getValidator().validate(check);

    if (constraints.size() != 0) {
      return List.of(format("Invalid email address '%s' for admin '%s %s'", user.getEmail().getValue(),
        user.getFirstName().getValue(), user.getLastName().getValue()));
    }

    return List.of();
  }

  private List<String> getErrors(Set<ConstraintViolation<ProgramEntity>> constraints) {
    Set<String> s = mapToSet(constraints, c -> c.getPropertyPath() + " " + c.getMessage());

    // remove redundant error message (null implies invalid)
    if (s.contains("shortName is invalid") && s.contains("shortName must not be null")) {
      s.remove("shortName is invalid");
    }

    return s.stream().sorted().collect(Collectors.toList());
  }

  @AllArgsConstructor
  class EmailCheck {
    @Email
    String email;
  }

  public List<String> validateProgram(@NonNull Program program) {
    val errors = new ArrayList<String>();

    val programEntity = ProgramConverterImpl.INSTANCE.programToProgramEntity(program);

    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    val constraints = validatorFactory.getValidator().validate(programEntity);
    if (constraints.size() != 0) {
      errors.addAll(getErrors(constraints));
    }

    if (program.getCancerTypesList().isEmpty()) {
      errors.add("Must include at least one cancerType");
    }

    if (program.getPrimarySitesList().isEmpty()) {
      errors.add("Must include at least one primarySite");
    }

    if (program.getRegionsList().isEmpty()) {
      errors.add("Must include at least one region");
    }

    if (program.getCountriesList().isEmpty()) {
      errors.add("Must include at least one country");
    }

    errors.addAll(invalidChoices("Invalid cancerType '%s'", validCancerTypes(),
      new TreeSet<>(program.getCancerTypesList())));

    errors.addAll(invalidChoices("Invalid primarySite '%s'", validPrimarySites(),
      new TreeSet<>((program.getPrimarySitesList()))));

    errors.addAll(invalidChoices("Invalid region '%s'", validRegions(),
      new TreeSet<>(program.getRegionsList())));

    errors.addAll(invalidChoices("Invalid country '%s'", validCountries(),
      new TreeSet<>(program.getCountriesList())));

    return errors;
  }

  private <T> List<String> invalidChoices(String fmt, Set<T> allowed, Set<T> actual) {
    return actual.stream().
      filter(choice -> !allowed.contains(choice)).
      map(badChoice -> format(fmt, badChoice)).
      collect(Collectors.toList());
  }

  public Set<String> validCancerTypes() {
    return mapToSet(programService.listCancers(), CancerEntity::getName);
  }

  public Set<String> validPrimarySites() {
    return mapToSet(programService.listPrimarySites(), PrimarySiteEntity::getName);
  }

  public Set<String> validCountries() {
    return mapToSet(programService.listCountries(), CountryEntity::getName);
  }

  public Set<String> validRegions() {
    return mapToSet(programService.listRegions(), RegionEntity::getName);
  }
}
