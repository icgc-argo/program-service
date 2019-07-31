package org.icgc.argo.program_service.services;

import io.grpc.Status;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.context.annotation.Profile;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Profile("auth")
@Slf4j
public class EgoAuthorizationService implements AuthorizationService {
  private String dccAdminPermission;

  public EgoAuthorizationService(String dccAdminPermission) {
    this.dccAdminPermission = dccAdminPermission;
    log.info(format("Created egoAuthorization service with permission='%s'",dccAdminPermission));
  }

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
    val permissions = getPermissions();

    return permissions.contains(dccAdminPermission);
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
