package org.icgc.argo.program_service.properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.ClockProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class MyClockProvider implements ClockProvider {
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
