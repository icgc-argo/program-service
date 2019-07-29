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
    // too long
    validate("12345678P-CA", false);
    // must contain dash before country code
    validate("12345678CA", false);
    // not ok (UK is not a valid country code )
    validate("TEST-UK", false);
    // GB is a valid country code (Great Britain)
    // dashes are legal (DK is Denmark)
    validate("---DK", false);
    // lowercase is invalid
    validate("testgb", false);

    validate("9A-FO3-CA", true);
    // underscore is allowed
    validate("9A_FOO3-CA", true);
    // Can only contain numbers
    validate("12345678-CA", true);
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
