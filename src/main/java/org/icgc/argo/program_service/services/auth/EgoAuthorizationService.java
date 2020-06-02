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

package org.icgc.argo.program_service.services.auth;

import static java.lang.String.format;

import io.grpc.Status;
import java.util.Collections;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.context.annotation.Profile;

@Profile("auth")
@Slf4j
public class EgoAuthorizationService implements AuthorizationService {
  private String dccAdminPermission;

  public EgoAuthorizationService(String dccAdminPermission) {
    this.dccAdminPermission = dccAdminPermission;
    log.info(
        format(
            "Created egoAuthorization service with dccAdmin permission='%s'", dccAdminPermission));
  }

  public EgoToken getEgoToken() {
    return EgoAuthInterceptor.EGO_TOKEN.get();
  }

  private EgoToken fromToken() {
    val token = getEgoToken();
    if (token == null) {
      log.warn("RPC call was not authenticated");
      throw Status.fromCode(Status.Code.UNAUTHENTICATED).asRuntimeException();
    }
    return token;
  }

  public boolean hasPermission(@NotNull String permission) {
    log.debug(format("Want permission: %s", permission));
    val status = getPermissions().contains(permission);
    log.debug(format("hasPermission returns %s", status));
    return status;
  }

  @Override
  public boolean isDCCAdmin() {
    val permissions = getPermissions();

    return permissions.contains(dccAdminPermission);
  }

  private Set<String> getPermissions() {
    val permissions = fromToken().getPermissions();

    if (permissions == null) {
      return Collections.unmodifiableSet(Collections.EMPTY_SET);
    }
    log.debug(format("Got permissions: %s", Set.of(permissions)));
    return Collections.unmodifiableSet(Set.of(permissions));
  }

  public boolean hasEmail(String email) {
    val authenticatedEmail = fromToken().getEmail();
    log.debug(format("Want email '%s'", email));
    log.debug(format("Have email '%s'", authenticatedEmail));

    if (authenticatedEmail == null || email == null) {
      return false;
    }

    return authenticatedEmail.equalsIgnoreCase(email);
  }

  public void require(boolean condition, String message) {
    if (!condition) {
      log.debug("Permission denied", message);
      throw Status.fromCode(Status.Code.PERMISSION_DENIED).asRuntimeException();
    }
  }
}
