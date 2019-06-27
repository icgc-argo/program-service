package org.icgc.argo.program_service.properties;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Map;

@Configuration
@Lazy
class SpringValidatorConfiguration {


  @Bean
  @Lazy
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(final ValidatorFactory factory) {
    return new HibernatePropertiesCustomizer() {

      @Override
      public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("javax.persistence.validation.factory", factory);
      }
    };
  }
}