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

package org.icgc.argo.program_service.services;

import static java.lang.String.format;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToSet;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Email;
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

@Service
@Validated
@Slf4j
public class ValidationService {

  /** Dependencies */
  private final ProgramService programService;

  private final ValidatorFactory validatorFactory;

  @Autowired
  public ValidationService(
      @NonNull ProgramService programService, @NonNull ValidatorFactory validatorFactory) {
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
      return List.of(
          format(
              "Invalid email address '%s' for admin '%s %s'",
              user.getEmail().getValue(),
              user.getFirstName().getValue(),
              user.getLastName().getValue()));
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
    @Email String email;
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

    if (program.getCountriesList().isEmpty()) {
      errors.add("Must include at least one country");
    }

    errors.addAll(
        invalidChoices(
            "Invalid cancerType '%s'",
            validCancerTypes(), new TreeSet<>(program.getCancerTypesList())));

    errors.addAll(
        invalidChoices(
            "Invalid primarySite '%s'",
            validPrimarySites(), new TreeSet<>((program.getPrimarySitesList()))));

    errors.addAll(
        invalidChoices(
            "Invalid country '%s'", validCountries(), new TreeSet<>(program.getCountriesList())));

    return errors;
  }

  private <T> List<String> invalidChoices(String fmt, Set<T> allowed, Set<T> actual) {
    return actual.stream()
        .filter(choice -> !allowed.contains(choice))
        .map(badChoice -> format(fmt, badChoice))
        .collect(Collectors.toList());
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
