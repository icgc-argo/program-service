package org.icgc.argo.program_service.services;

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

  EgoUser createEgoUser(String email);

  EgoPolicy createEgoPolicy(String policyName);

  EgoGroup ensureGroupExists(String groupName);

  Optional<EgoGroup> getGroup(String groupName);

  Optional<EgoUser> getUser(@Email String email);

  Stream<EgoUser> getUsersByGroup(UUID groupId);

  void deleteGroup(UUID egoGroupId);

  void deletePolicy(UUID policyId);

  Optional<EgoPolicy> getPolicyByName(String name);

  void removePolicyByName(String name);

  void addUserToGroup(UUID egoGroupId, UUID egoUserId);

  void removeUserFromGroup(UUID egoGroupId, UUID userId);

  boolean isMember(UUID groupId, String email);

  EgoPermission[] getGroupPermissions(UUID groupId);
}
