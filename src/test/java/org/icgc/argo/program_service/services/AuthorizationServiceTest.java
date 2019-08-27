package org.icgc.argo.program_service.services;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.val;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.junit.jupiter.api.Test;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {
  AuthorizationService authorizationService(EgoToken token) {
    return new EgoAuthorizationService("PROGRAMSERVICE.WRITE") {
      @Override
      public EgoToken getEgoToken() {
        return token;
      }
    };
  }

  EgoToken mockToken(String type, String... permissions) {
    val token = mock(EgoToken.class);
    when(token.getType()).thenReturn(type);
    when(token.getPermissions()).thenReturn(permissions);
    return token;
  }

  void assertUnauthenticated(Exception exception) {
    assertNotNull(exception);
    assertTrue(exception instanceof StatusRuntimeException);
    assertEquals(((StatusRuntimeException) exception).getStatus(), Status.UNAUTHENTICATED);
  }

  void assertPermissionDenied(Exception exception) {
    assertNotNull(exception);
    assertTrue(exception instanceof StatusRuntimeException);
    assertEquals(((StatusRuntimeException) exception).getStatus(), Status.PERMISSION_DENIED);
  }

  @Test
  void requireDCCAdmin() {
    // No authorization provided
    val exception = testRequireDCCAdmin(null);
    assertUnauthenticated(exception);

    // Wrong authorization (null field)
    val exception2 = testRequireDCCAdmin(mock(EgoToken.class));
    assertPermissionDenied(exception2);

    // Wrong authorization (wrong type)
    val exception3 = testRequireDCCAdmin(mockToken("USER"));
    assertPermissionDenied(exception3);

    // Ensure that Ego ADMIN doesn't mean DCC ADMIN
    val exception4 = testRequireDCCAdmin(mockToken("ADMIN"));
    assertPermissionDenied(exception4);

    // Write access to some program doesn't mean DCC Admin
    val exception5 = testRequireDCCAdmin(mockToken("USER", "PROGRAM-ABC.WRITE"));
    assertPermissionDenied(exception5);

    val exception6 = testRequireDCCAdmin(mockToken("USER", "PROGRAMSERVICE.WRITE"));
    assertNull(exception6);
  }

  private Exception testRequireDCCAdmin(EgoToken token) {
    val auth = authorizationService(token);
    Exception exception = null;
    try {
      auth.requireDCCAdmin();
    } catch (Exception ex) {
      exception = ex;
    }
    return exception;
  }

  @Test
  void requireProgramAdmin() {
    val program = "TEST-CA";
    val readPermission = "PROGRAM-TEST-CA.READ";
    val writePermission = "PROGRAM-TEST-CA.WRITE";
    val dccAdminPermission = "PROGRAMSERVICE.WRITE";
    val exception = testRequireProgramAdmin(null, program);
    assertUnauthenticated(exception);

    // Wrong authorization (null field)
    val exception2 = testRequireProgramAdmin(mock(EgoToken.class), program);
    assertPermissionDenied(exception2);

    // Authorization ok (null program name)
    val exception3 = testRequireProgramAdmin(mockToken("USER"), program);
    assertPermissionDenied(exception3);

    // No permissions
    val exception4 = testRequireProgramAdmin(mockToken("USER"), program);
    assertPermissionDenied(exception4);

    // Ego Admin, No permissions
    val exception5 = testRequireProgramAdmin(mockToken("ADMIN"), program);
    assertPermissionDenied(exception5);

    // Right program, wrong permission
    val exception6 = testRequireProgramAdmin(mockToken("USER", readPermission), program);
    assertPermissionDenied(exception6);

    // Right permission, wrong program
    val exception7 = testRequireProgramAdmin(mockToken("USER", "PROGRAM-TEST-GB.WRITE"), program);
    assertPermissionDenied(exception7);

    // Right permission, right program
    val exception8 = testRequireProgramAdmin(mockToken("USER", readPermission, writePermission), program);
    assertNull(exception8);

    // Right permission, null program
    val exception9 = testRequireProgramAdmin(mockToken("USER", readPermission, writePermission),
      null);
    assertPermissionDenied(exception9);

    // DCC Admin, null program
    val exception10 = testRequireProgramAdmin(mockToken("ADMIN", dccAdminPermission), null);
    assertNull(exception10);

    // DCC Admin, right program
    val exception11 = testRequireProgramAdmin(mockToken("ADMIN", dccAdminPermission), null);
    assertNull(exception11);
  }

  private Exception testRequireProgramAdmin(EgoToken token, String programShortName) {
    val auth = authorizationService(token);
    Exception exception = null;
    try {
      auth.requireProgramAdmin(programShortName);
    } catch (Exception ex) {
      exception = ex;
    }
    return exception;
  }

  @Test
  void requireProgramUser() {
    val program = "TEST-CA";
    val readPermission = "PROGRAM-TEST-CA.READ";
    val writePermission = "PROGRAM-TEST-CA.WRITE";
    val dccAdminPermission = "PROGRAMSERVICE.WRITE";

    val exception = testRequireProgramUser(null, program);
    assertUnauthenticated(exception);

    // Wrong authorization (null field)
    val exception2 = testRequireProgramUser(mock(EgoToken.class), program);
    assertPermissionDenied(exception2);

    // Authorization ok (null program name)
    val exception3 = testRequireProgramUser(mockToken("USER"), program);
    assertPermissionDenied(exception3);

    // No permissions
    val exception4 = testRequireProgramUser(mockToken("USER"), program);
    assertPermissionDenied(exception4);

    // EGO Admin, No permissions
    val exception5 = testRequireProgramUser(mockToken("ADMIN"), program);
    assertPermissionDenied(exception5);

    // Right program, right permission
    val exception6 = testRequireProgramUser(mockToken("USER", readPermission), program);
    assertNull(exception6);

    // Wrong permission, wrong program
    val exception7 = testRequireProgramUser(mockToken("USER", "PROGRAM-TEST-GB.READ",
      "PROGRAM-TEST-GB.WRITE"), program);
    assertPermissionDenied(exception7);

    // Wrong permission, right program
    val exception8 = testRequireProgramUser(mockToken("USER", readPermission, writePermission), program);
    assertNull(exception8);

    // null program
    val exception9 = testRequireProgramUser(mockToken("USER", readPermission, writePermission),
      null);
    assertPermissionDenied(exception9);

    // DCC permissions, null program
    val exception10 = testRequireProgramUser(mockToken("USER", dccAdminPermission), null);
    assertNull(exception10);

    // DCC Admin, right program
    val exception11 = testRequireProgramAdmin(mockToken("USER", dccAdminPermission), program);
    assertNull(exception11);
  }

  private Exception testRequireProgramUser(EgoToken token, String programShortName) {
    val auth = authorizationService(token);
    Exception exception = null;
    try {
      auth.requireProgramUser(programShortName);
    } catch (Exception ex) {
      exception = ex;
    }
    return exception;
  }

  @Test
  void requireEmail() {
    val email = "n@ai";

    val exception1 = testRequireEmail(null, email);
    assertUnauthenticated(exception1);

    val exception2 = testRequireEmail(mock(EgoToken.class), email);
    assertPermissionDenied(exception2);

    val token1 = mock(EgoToken.class);
    when(token1.getEmail()).thenReturn("wrong@ai");
    val exception3 = testRequireEmail(token1, email);
    assertPermissionDenied(exception3);

    val token2 = mock(EgoToken.class);
    when(token2.getEmail()).thenReturn(email);
    val exception4 = testRequireEmail(token2, email);
    assertNull(exception4);

    val exception5 = testRequireEmail(token2, null);
    assertPermissionDenied(exception5);
  }

  private Exception testRequireEmail(EgoToken token, String email) {
    val auth = authorizationService(token);
    Exception exception = null;
    try {
      auth.requireEmail(email);
    } catch (Exception ex) {
      exception = ex;
    }
    return exception;
  }
}
