package org.icgc.argo.program_service.services.ego;

import org.icgc.argo.program_service.services.ego.model.entity.EgoGroup;
import org.icgc.argo.program_service.services.ego.model.entity.EgoPermission;
import org.icgc.argo.program_service.services.ego.model.entity.EgoPolicy;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;

import javax.validation.constraints.Email;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface EgoClient {
  RSAPublicKey getPublicKey();

  void assignPermission(EgoGroup group, EgoPolicy policy, String mask);

  EgoUser createEgoUser(String email, String firstname, String lastname);

  EgoPolicy createEgoPolicy(String policyName);

  EgoGroup ensureGroupExists(String groupName);

  Optional<EgoGroup> getGroupByName(String groupName);

  Optional<EgoUser> getUser(@Email String email);
  EgoUser getUserById(UUID userId);
  void deleteUserById(UUID userId);

  Stream<EgoUser> getUsersByGroupId(UUID groupId);
  Stream <EgoGroup> getGroupsByUserId(UUID userId);

  void deleteGroup(UUID egoGroupId);
  void deletePolicy(UUID policyId);

  Optional<EgoPolicy> getPolicyByName(String name);

  void removePolicyByName(String name);

  void addUserToGroup(UUID egoGroupId, UUID egoUserId);
  void removeUserFromGroup(UUID egoGroupId, UUID userId);

  boolean isMember(UUID groupId, String email);

  EgoPermission[] getGroupPermissions(UUID groupId);
}
