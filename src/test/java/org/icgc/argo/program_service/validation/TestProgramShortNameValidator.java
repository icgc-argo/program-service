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

package org.icgc.argo.program_service.validation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

import static junit.framework.TestCase.assertEquals;

class TestProgramShortNameValidator {
  ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
  Validator validator = validatorFactory.getValidator();


  @Test
  public void testValidator() {
    // contains invalid characters (numbers)
    validate("ABC123", false);
    // too short
    validate("CA", false);
    // ok (Canada is a valid country code)
    validate("TEST-CA", true);
    // not ok (UK is not a valid country code )
    validate("TEST-UK", false);
    // GB is a valid country code (Great Britain)
    // Short name must have dash
    validate("TESTGB", false);
    // dashes are legal (DK is Denmark)
    validate("---DK", false);
    // lowercase is invalid
    validate("testgb", false);
  }

  private void validate(String name, boolean ok) {
    int expected = 0;
    if (!ok) {
      expected = 1;
    }
    val name1 = new NameTest(name);
    val constraintViolations = validator.validate(name1);
    assertEquals(expected, constraintViolations.size());
  }
}

@ValidateOnExecution(type = ExecutableType.ALL)
@AllArgsConstructor
@Data
@Valid
class NameTest {
  @ProgramShortName
  String name;
}
