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

public interface AuthorizationService {
  boolean isDCCAdmin();

  boolean hasPermission(String permission);

  boolean hasEmail(String email);

  private String readPermission(String programShortName) {
    return "PROGRAM-" + programShortName + ".READ";
  }

  private String writePermission(String programShortName) {
    return "PROGRAM-" + programShortName + ".WRITE";
  }

  default void require(boolean condition, String message) {
    if (!condition) {
      throw Status.PERMISSION_DENIED.augmentDescription(message).asRuntimeException();
    }
  }

  default void requireDCCAdmin() {
    require(isDCCAdmin(), "Not signed in as a DCC Administrator");
  }

  default void requireProgramAdmin(String programShortName) {
    require(
        canWrite(programShortName), format("No WRITE permission for program %s", programShortName));
  }

  default void requireProgramUser(String programShortName) {
    require(
        canRead(programShortName), format("NO READ permission for program %s", programShortName));
  }

  default boolean canRead(String programShortName) {
    return isAuthorized(readPermission(programShortName))
        || isAuthorized(writePermission(programShortName));
  }

  default boolean canWrite(String programShortName) {
    return isAuthorized(writePermission(programShortName));
  }

  default void requireEmail(String email) {
    require(hasEmail(email), format("is not signed in as user '%s'", email));
  }

  default boolean isAuthorized(String permission) {
    return isDCCAdmin() || hasPermission(permission);
  }
}
