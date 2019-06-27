package org.icgc.argo.program_service.properties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.ClockProvider;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

@Slf4j
@Configuration
public class ValidationProperties {
  @Bean
  public ClockProvider clockProvider() {
    return new UTCClockProvider();
  }


  @Bean
  public ValidatorFactory factory() {
    return Validation.byDefaultProvider()
      .configure()
      .clockProvider(clockProvider())
      .buildValidatorFactory();
  }

}

