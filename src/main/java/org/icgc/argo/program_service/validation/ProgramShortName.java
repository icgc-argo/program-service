package org.icgc.argo.program_service.validation;


import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Target({ FIELD })
@Retention(RUNTIME)
@Constraint(validatedBy = ProgramShortNameValidator.class)
@Documented
public @interface ProgramShortName {
  String message() default "{ProgramShortName.invalid}";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
