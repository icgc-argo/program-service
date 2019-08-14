package org.icgc.argo.program_service.services;

import io.grpc.Status;

import static java.lang.String.format;

public interface  AuthorizationService {
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
    require(isDCCAdmin(), "not dCCAdmin");
  }

  default void requirePermission(String permission) {
    require(isAuthorized(permission), format("does not have permission '%s'", permission));
  }

  default void requireProgramAdmin(String programShortName) {
    requirePermission(writePermission(programShortName));
  }

  default void requireProgramUser(String programShortName) {
    requirePermission(readPermission(programShortName));
  }

  default boolean canRead(String programShortName) {
    return isAuthorized(readPermission(programShortName)) || isAuthorized(writePermission(programShortName));
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
