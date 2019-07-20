package org.icgc.argo.program_service.services;

import io.grpc.Status;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;

public class AuthorizationService {
  private final EgoToken token;

  public AuthorizationService(EgoToken token) {
    this.token = token;
  }
  
  private String readPermission(String programShortName) {
    return "PROGRAM-"+ programShortName +".READ";
  }

  private String writePermission(String programShortName) {
    return "PROGRAM-" + programShortName + ".WRITE";
  }

  private EgoToken getToken() {
    if (token == null) {
      throw Status.fromCode(Status.Code.UNAUTHENTICATED).asRuntimeException();
    }
    return token;
  }

  public void require(boolean condition) {
    if (!condition) {
      throw Status.fromCode(Status.Code.PERMISSION_DENIED).asRuntimeException();
    }
  }

  public void requireDCCAdmin() {
    require(isDCCAdmin());
  }

  public void requirePermission(String permission) {
    require(isAuthorized(permission));
  }

  public void requireProgramAdmin(String programShortName) {
    require(canWrite(programShortName));
  }

  public void requireProgramUser(String programShortName) {
    require(canRead(programShortName));
  }

  public boolean canRead(String programShortName) {
    return isAuthorized(readPermission(programShortName));
  }

  public boolean canWrite(String programShortName) {
    return isAuthorized(writePermission(programShortName));
  }

  public void requireEmail(String email) {
    require(hasEmail(email));
  }

  public boolean isDCCAdmin() {
    return getToken().getType().equalsIgnoreCase("ADMIN");
  }

  public boolean isAuthorized(String permission) {
    return isDCCAdmin() || hasPermission(permission);
  }

  public boolean hasPermission(String permission) {
    return getPermissions().contains(permission);
  }

  private Set<String> getPermissions() {
    return new HashSet<>(Arrays.asList(getToken().getPermissions()));
  }

  public boolean hasEmail(String email) {
    return getToken().getEmail().equalsIgnoreCase(email);
  }

}
