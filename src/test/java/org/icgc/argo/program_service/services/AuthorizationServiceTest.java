package org.icgc.argo.program_service.services;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.val;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.*;

public class AuthorizationServiceTest {
  AuthorizationService authorizationService(EgoToken token) {
    return new EgoAuthorizationService(){
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
    assertThat(exception).isNotNull();
    assertThat(exception).isInstanceOf(StatusRuntimeException.class);
    assertThat(((StatusRuntimeException) exception).getStatus().equals(Status.UNAUTHENTICATED)).isTrue();
  }

  void assertPermissionDenied(Exception exception) {
    assertThat(exception).isNotNull();
    assertThat(exception).isInstanceOf(StatusRuntimeException.class);
    assertThat(((StatusRuntimeException) exception).getStatus().equals(Status.PERMISSION_DENIED)).isTrue();
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

    // Right Authorization
    val exception4 = testRequireDCCAdmin(mockToken("ADMIN"));
    assertThat(exception4).isNull();
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
    val program="TEST-CA";
    val readPermission = "PROGRAM-TEST-CA.READ";
    val writePermission = "PROGRAM-TEST-CA.WRITE";
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

    // DCC Admin, No permissions
    val exception5 = testRequireProgramAdmin(mockToken("ADMIN"), program);
    assertThat(exception5).isNull();

    // Right program, wrong permission
    val exception6 = testRequireProgramAdmin(mockToken("USER",readPermission), program);
    assertPermissionDenied(exception6);

    // Right permission, wrong program
    val exception7 = testRequireProgramAdmin(mockToken("USER","PROGRAM-TEST-GB.WRITE"), program);
    assertPermissionDenied(exception7);

    // Right permission, right program
    val exception8 = testRequireProgramAdmin(mockToken("USER",readPermission, writePermission), program);
    assertThat(exception8).isNull();

    // Right permission, null program
    val exception9 = testRequireProgramAdmin(mockToken("USER",readPermission, writePermission),
      null);
    assertPermissionDenied(exception9);

    // DCC permissions, null program
    val exception10 = testRequireProgramAdmin(mockToken("ADMIN"), null);
    assertThat(exception10).isNull();
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
    val program="TEST-CA";
    val readPermission = "PROGRAM-TEST-CA.READ";
    val writePermission = "PROGRAM-TEST-CA.WRITE";
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

    // DCC Admin, No permissions
    val exception5 = testRequireProgramUser(mockToken("ADMIN"), program);
    assertThat(exception5).isNull();

    // Right program, right permission
    val exception6 = testRequireProgramUser(mockToken("USER",readPermission), program);
    assertThat(exception6).isNull();

    // Wrong permission, wrong program
    val exception7 = testRequireProgramUser(mockToken("USER", "PROGRAM-TEST-GB.READ",
      "PROGRAM-TEST-GB.WRITE"), program);
    assertPermissionDenied(exception7);

    // Wrong permission, right program
    val exception8 = testRequireProgramUser(mockToken("USER",readPermission, writePermission), program);
    assertThat(exception8).isNull();

    // null program
    val exception9 = testRequireProgramUser(mockToken("USER",readPermission, writePermission),
      null);
    assertPermissionDenied(exception9);

    // DCC permissions, null program
    val exception10 = testRequireProgramUser(mockToken("ADMIN"), null);
    assertThat(exception10).isNull();
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
    assertThat(exception4).isNull();

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
