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
