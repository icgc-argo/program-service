package org.icgc.argo.program_service.services;

import io.grpc.Status;

public interface AuthorizationService {
  boolean isDCCAdmin();
  boolean hasPermission(String permission);
  boolean hasEmail(String email);

  private String readPermission(String programShortName) {
    return "PROGRAM-"+ programShortName +".READ";
  }

  private String writePermission(String programShortName) {
    return "PROGRAM-" + programShortName + ".WRITE";
  }

  default void require(boolean condition) {
    if (!condition) {
      throw Status.fromCode(Status.Code.PERMISSION_DENIED).asRuntimeException();
    }
  }

  default void requireDCCAdmin() {
    require(isDCCAdmin());
  }

  default void requirePermission(String permission) {
    require(isAuthorized(permission));
  }

  default void requireProgramAdmin(String programShortName) {
    require(canWrite(programShortName));
  }

  default void requireProgramUser(String programShortName) {
    require(canRead(programShortName));
  }

  default boolean canRead(String programShortName) {
    return isAuthorized(readPermission(programShortName));
  }

  default boolean canWrite(String programShortName) {
    return isAuthorized(writePermission(programShortName));
  }

  default void requireEmail(String email) {
    require(hasEmail(email));
  }

  default boolean isAuthorized(String permission) {
    return isDCCAdmin() || hasPermission(permission);
  }
}
