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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.ClockProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;


@Configuration
public class UTCClockProvider implements ClockProvider {
  @Override
  @Bean
  public Clock getClock() {

    return new Clock() {
      @Override public ZoneId getZone() {
        return ZoneId.of("UTC");
      }

      @Override public Clock withZone(ZoneId zone) {
        return Clock.system(ZoneId.of("UTC"));
      }

      @Override public Instant instant() {
        return Instant.now();
      }
    };
  }
}
