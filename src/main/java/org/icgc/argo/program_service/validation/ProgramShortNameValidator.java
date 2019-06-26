package org.icgc.argo.program_service.validation;

import lombok.val;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class ProgramShortNameValidator implements ConstraintValidator<ProgramShortName, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    val pattern = Pattern.compile("^[-A-Z]+([A-Z][A-Z])$");
    val matcher = pattern.matcher(value);
    val countries = new TreeSet<String>(Arrays.asList(Locale.getISOCountries()));

    if (!matcher.matches()) {
      return false;
    }

    val countrycode = matcher.group(1);

    return countries.contains(countrycode);
  }

}
