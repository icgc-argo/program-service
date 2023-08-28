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

import java.util.Collections;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.model.exceptions.ForbiddenException;
import org.icgc.argo.program_service.model.exceptions.UnauthorizedException;
import org.icgc.argo.program_service.security.EgoRestSecurity;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Profile("auth")
@Service
@Slf4j
public class EgoRestAuthorizationService implements RestAuthorizationService {

  private final EgoRestSecurity egoSecurity;

  @Value("${app.dccAdminPermission}")
  private String dccAdminPermission;

  private final EgoClient client;

  @Autowired
  public EgoRestAuthorizationService(
      @NonNull EgoRestSecurity egoSecurity, @NotNull EgoClient client) {
    this.egoSecurity = egoSecurity;
    this.client = client;
  }

  @Override
  public void requireDCCAdmin(String jwtToken) {
    require(isDCCAdmin(jwtToken), "Not signed in as a DCC Administrator");
  }

  @Override
  public void requireProgramUser(String programShortName, String jwtToken) {
    require(
        canRead(programShortName, jwtToken),
        format("NO READ permission for program %s", programShortName));
  }

  @Override
  public boolean canRead(String programShortName, String jwtToken) {
    return isAuthorized(readPermission(programShortName), jwtToken)
        || isAuthorized(writePermission(programShortName), jwtToken);
  }

  private boolean isDCCAdmin(String jwtToken) {
    val permissions = getPermissions(jwtToken);

    return permissions.contains(dccAdminPermission);
  }

  private Set<String> getPermissions(String jwtToken) {
    val permissions = fromToken(jwtToken).getPermissions();

    if (permissions == null) {
      return Collections.unmodifiableSet(Collections.EMPTY_SET);
    }
    log.debug(format("Got permissions: %s", Set.of(permissions)));
    return Collections.unmodifiableSet(Set.of(permissions));
  }

  private boolean hasEmail(String email, String jwtToken) {
    val authenticatedEmail = fromToken(jwtToken).getEmail();
    log.debug(format("Want email '%s'", email));
    log.debug(format("Have email '%s'", authenticatedEmail));

    if (authenticatedEmail == null || email == null) {
      return false;
    }

    return authenticatedEmail.equalsIgnoreCase(email);
  }

  private void require(boolean condition, String message) {
    if (!condition) {
      log.debug("Permission denied", message);
      throw new ForbiddenException("Permission Denied");
    }
  }

  private boolean isAuthorized(String permission, String jwtToken) {
    return isDCCAdmin(jwtToken) || hasPermission(permission, jwtToken);
  }

  private EgoToken fromToken(String jwtToken) {
    EgoRestSecurity egoSecurity = new EgoRestSecurity(client.getPublicKey());
    val token = egoSecurity.verifyRestTokenHeader(jwtToken.replace("Bearer", "").trim());
    if (token == null) {
      log.warn("Rest call was not authenticated");
      throw new UnauthorizedException("No Token or Invalid Token");
    }
    return token.get();
  }

  private boolean hasPermission(@NotNull String permission, String jwtToken) {
    log.debug(format("Want permission: %s", permission));
    val status = getPermissions(jwtToken).contains(permission);
    log.debug(format("hasPermission returns %s", status));
    return status;
  }

  private String readPermission(String programShortName) {
    return "PROGRAM-" + programShortName + ".READ";
  }

  private String writePermission(String programShortName) {
    return "PROGRAM-" + programShortName + ".WRITE";
  }
}
