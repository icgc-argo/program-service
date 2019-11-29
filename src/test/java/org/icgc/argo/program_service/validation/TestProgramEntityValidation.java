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

import static junit.framework.TestCase.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.PastOrPresent;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;
import org.icgc.argo.program_service.properties.ValidationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

@Component
public class TestProgramEntityValidation {
  ValidationProperties properties = new ValidationProperties();
  ValidatorFactory validatorFactory = properties.factory();
  Validator validator = validatorFactory.getValidator();

  @Test
  public void testValidator() {
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    val name1 = new DateTest(now, now.plusHours(12));
    val constraintViolations = validator.validate(name1);
    assertEquals(0, constraintViolations.size());
  }
}

@ValidateOnExecution(type = ExecutableType.ALL)
@AllArgsConstructor
@Data
@Valid
class DateTest {
  @PastOrPresent LocalDateTime updatedAt;
  @FutureOrPresent LocalDateTime expiresAt;
}
