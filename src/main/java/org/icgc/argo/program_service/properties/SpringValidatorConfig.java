package org.icgc.argo.program_service.properties;

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
 */
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Map;

// This class configures hibernate to use get the default ValidationFactory from a Bean called "factory",
// instead of using it's own default.
//
// We need this so that our Bean can use a ValidationFactory with a Clock setting that always uses UTC time.
//
//See: https://stackoverflow.com/questions/50212117/spring-boot-hibernate-custom-constraint-doesnt-inject-service/50213178#50213178
// and https://stackoverflow.com/questions/2712345/jsr-303-dependency-injection-and-hibernate
// for more details on how this configuration works.
//
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