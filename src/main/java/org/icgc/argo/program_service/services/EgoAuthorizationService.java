package org.icgc.argo.program_service.services;

import io.grpc.Status;
import lombok.val;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Profile("auth")
@Service
public class EgoAuthorizationService implements AuthorizationService {
  public EgoToken getEgoToken() {
    return EgoAuthInterceptor.EGO_TOKEN.get();
  }

  private EgoToken fromToken() {
    val token = getEgoToken();
    if (token == null) {
      throw Status.fromCode(Status.Code.UNAUTHENTICATED).asRuntimeException();
    }
    return token;
  }

  public boolean hasPermission(String permission) {
    return getPermissions().contains(permission);
  }

  @Override
  public boolean isDCCAdmin(){
    val type = fromToken().getType();

    if (type == null) {
      return false;
    }
    return type.equalsIgnoreCase("ADMIN");
  }

  private Set<String> getPermissions() {
    val permissions = fromToken().getPermissions();

    if (permissions == null) {
      return new HashSet<>();
    }
    return new HashSet<>(Arrays.asList(permissions));
  }

  public boolean hasEmail(String email) {
    val authenticatedEmail = fromToken().getEmail();

    if (authenticatedEmail == null || email == null) {
      return false;
    }

    return authenticatedEmail.equalsIgnoreCase(email);
  }

}
