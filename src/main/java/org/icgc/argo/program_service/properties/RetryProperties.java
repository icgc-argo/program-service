/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */
package org.icgc.argo.program_service.properties;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.retry.backoff.ExponentialBackOffPolicy.DEFAULT_MULTIPLIER;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import org.icgc.argo.program_service.retry.DefaultRetryListener;
import org.icgc.argo.program_service.retry.RetryPolicies;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Component
@ConfigurationProperties("retry")
public class RetryProperties {

  private final Retry connection = new Retry();

  @Bean
  @Primary
  public RetryTemplate retryTemplate() {
    return buildRetryTemplate(false);
  }

  @Bean
  public RetryTemplate lenientRetryTemplate() {
    return buildRetryTemplate(true);
  }

  private RetryTemplate buildRetryTemplate(boolean retryOnAllErrors) {
    val result = new RetryTemplate();
    result.setBackOffPolicy(defineBackOffPolicy());

    result.setRetryPolicy(
        new SimpleRetryPolicy(
            connection.getMaxRetries(), RetryPolicies.getRetryableExceptions(), true));
    result.registerListener(new DefaultRetryListener(retryOnAllErrors));
    return result;
  }

  private BackOffPolicy defineBackOffPolicy() {
    val backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(connection.getInitialBackoff());
    backOffPolicy.setMultiplier(connection.getMultiplier());

    return backOffPolicy;
  }

  @Getter
  @Setter
  @Validated
  private static class Retry {

    private static final int DEFAULT_MAX_RETRIES = 5;
    private static final long DEFAULT_INITIAL_BACKOFF_INTERVAL = SECONDS.toMillis(15L);

    @NotNull @PositiveOrZero private Integer maxRetries = DEFAULT_MAX_RETRIES;

    @NotNull @PositiveOrZero private Long initialBackoff = DEFAULT_INITIAL_BACKOFF_INTERVAL;

    @NotNull @PositiveOrZero private Double multiplier = DEFAULT_MULTIPLIER;
  }
}
