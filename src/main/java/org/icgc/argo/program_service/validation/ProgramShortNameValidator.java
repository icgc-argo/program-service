package org.icgc.argo.program_service.validation;

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

import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import lombok.val;

public class ProgramShortNameValidator implements ConstraintValidator<ProgramShortName, String> {

  private static final Pattern pattern =
      Pattern.compile("^[A-Z0-9][-_A-Z0-9]{2,7}[-]([A-Z][A-Z])$");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) {
      return false;
    }
    val matcher = pattern.matcher(value);
    val countries = new TreeSet<String>(Arrays.asList(Locale.getISOCountries()));

    if (!matcher.matches()) {
      return false;
    }

    val countrycode = matcher.group(1);

    return countries.contains(countrycode);
  }
}
