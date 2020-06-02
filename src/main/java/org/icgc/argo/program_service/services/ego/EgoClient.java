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

package org.icgc.argo.program_service.services.ego;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.validation.constraints.Email;
import org.icgc.argo.program_service.services.ego.model.entity.*;

public interface EgoClient {
  RSAPublicKey getPublicKey();

  void assignGroupPermissions(List<EgoGroupPermissionRequest> permissionRequests);

  void massDelete(EgoMassDeleteRequest request);

  EgoUser createEgoUser(String email, String firstname, String lastname);

  Optional<EgoGroup> getGroupByName(String groupName);

  Optional<EgoUser> getUser(@Email String email);

  EgoUser getUserById(UUID userId);

  void deleteUserById(UUID userId);

  Stream<EgoUser> getUsersByGroupId(UUID groupId);

  Stream<EgoGroup> getGroupsByUserId(UUID userId);

  void deleteGroup(UUID egoGroupId);

  void deletePolicy(UUID policyId);

  Optional<EgoPolicy> getPolicyByName(String name);

  void removePolicyByName(String name);

  void addUserToGroup(UUID egoGroupId, UUID egoUserId);

  void removeUserFromGroup(UUID egoGroupId, UUID userId);

  boolean isMember(UUID groupId, String email);

  EgoPermission[] getGroupPermissions(UUID groupId);

  EgoPermission[] getUserResolvedPermissions(UUID userId);
}
