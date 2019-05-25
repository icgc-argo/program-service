package org.icgc.argo.program_service.clients;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.icgc.argo.program_service.model.ego.Group;
import org.icgc.argo.program_service.model.ego.PermissionRequest;
import org.icgc.argo.program_service.model.ego.Policy;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.UUID;

@RequiredArgsConstructor
public class EgoClient {

  private final RestTemplate restTemplate;
  private final String apiUrl;

  public String getPublicKey(){
    return null;
  }

  public void deleteGroup(@NonNull UUID groupId){
  }

  public void deletePolicy(@NonNull UUID policyId){

  }

  public Policy createPolicy(@NonNull String policyName ){
    return null;
  }

  public Group createGroup(@NonNull String groupName, @NonNull String status ){
    return null;

  }

  public void createGroupPermission(@NonNull UUID policyId, @NonNull UUID groupId,
      @NonNull PermissionRequest permissionRequest){

  }

  public void addUsersToGroup(@NonNull Collection<UUID> userIds, @NonNull UUID groupId){

  }

  public void deleteUserFromGroup(@NonNull UUID userId, @NonNull UUID groupId){

  }

  public void listUsers(int offset, int limit, String query){

  }

  public void listGroups(int offset, int limit, String query){

  }

  public void listPolicies(int offset, int limit, String name){

  }

}
