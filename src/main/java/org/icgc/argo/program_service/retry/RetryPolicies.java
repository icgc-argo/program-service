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
package org.icgc.argo.program_service.retry;

import static java.lang.Boolean.TRUE;
import static lombok.AccessLevel.PRIVATE;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@NoArgsConstructor(access = PRIVATE)
public final class RetryPolicies {

  /**
   * Returns a ProgramEntityToProgramDetails with exceptions that should be retried by the Spring
   * Retry Framework.
   *
   * <ul>
   *   <li><b>ResourceAccessException</b> - to retry Connection Timeout
   *   <li><b>HttpServerErrorException</b> - to retry 503 Service Unavailable
   * </ul>
   */
  public static Map<Class<? extends Throwable>, Boolean> getRetryableExceptions() {
    return ImmutableMap.of(
        ResourceAccessException.class, TRUE,
        HttpServerErrorException.class, TRUE);
  }
}
