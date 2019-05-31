/*
 * Copyright (c) 2018. Ontario Institute for Cancer Research
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
package org.icgc.argo.program_service.config;

import lombok.val;
import org.icgc.argo.program_service.retry.DefaultRetryListener;
import org.icgc.argo.program_service.retry.RetryPolicies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.retry.backoff.ExponentialBackOffPolicy.DEFAULT_MULTIPLIER;

@Configuration
public class RetryConfig {

  private static final int DEFAULT_MAX_RETRIES = 5;
  private static final long DEFAULT_INITIAL_BACKOFF_INTERVAL = SECONDS.toMillis(15L);

  //TODO: [rtisma] replace with ConfigurationProperties
  @Value("${retry.connection.maxRetries}")
  private int maxRetries = DEFAULT_MAX_RETRIES;

  @Value("${retry.connection.initialBackoff}")
  private long initialBackoff = DEFAULT_INITIAL_BACKOFF_INTERVAL;

  @Value("${retry.connection.multiplier}")
  private double multiplier = DEFAULT_MULTIPLIER;

  @Bean
  @Primary
  public RetryTemplate retryTemplate() {
    return buildRetryTemplate(false);
  }

  @Bean
  public RetryTemplate lenientRetryTemplate() {
    return buildRetryTemplate(true);
  }

  private RetryTemplate buildRetryTemplate(boolean retryOnAllErrors){
    val result = new RetryTemplate();
    result.setBackOffPolicy(defineBackOffPolicy());

    result.setRetryPolicy(new SimpleRetryPolicy(maxRetries, RetryPolicies.getRetryableExceptions(), true));
    result.registerListener(new DefaultRetryListener(retryOnAllErrors));
    return result;

  }

  private BackOffPolicy defineBackOffPolicy() {
    val backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(initialBackoff);
    backOffPolicy.setMultiplier(multiplier);

    return backOffPolicy;
  }

}
