/*
 * Copyright (c) 2023 The Ontario Institute for Cancer Research. All rights reserved
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

package org.icgc.argo.program_service.security;

import io.grpc.Context;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Slf4j
@Component
@Profile("auth")
public class JWTAuthorizationFilter extends OncePerRequestFilter {

  private final EgoRestSecurity egoSecurity;
  private final EgoClient client;
  private String TOKEN_PREFIX = "Bearer";

  @Autowired
  @Qualifier("handlerExceptionResolver")
  private HandlerExceptionResolver resolver;

  public static final Context.Key<EgoToken> EGO_TOKEN = Context.key("egoToken");

  @Autowired
  public JWTAuthorizationFilter(@NonNull EgoRestSecurity egoSecurity, @NotNull EgoClient client) {
    this.egoSecurity = egoSecurity;
    this.client = client;
  }

  @SneakyThrows
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException, RuntimeException {

    try {
      val tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);
      EgoRestSecurity egoSecurity = new EgoRestSecurity(client.getPublicKey());
      if (tokenPayload != null) {
        val egoToken = egoSecurity.verifyRestTokenHeader(removeTokenPrefix(tokenPayload));
      }
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      log.error(ExceptionUtils.getStackTrace(e));
      resolver.resolveException(request, response, null, e);
    }
  }

  private String removeTokenPrefix(String token) {
    return token.replace(TOKEN_PREFIX, "").trim();
  }
}
