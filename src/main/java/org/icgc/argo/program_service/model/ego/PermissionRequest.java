package org.icgc.argo.program_service.model.ego;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@RequiredArgsConstructor(access = PRIVATE)
public class PermissionRequest {

  @NonNull private final String mask;

  public static PermissionRequest createReadPermissionRequest(){
    return new PermissionRequest("READ");
  }

  public static PermissionRequest createWritePermissionRequest(){
    return new PermissionRequest("WRITE");
  }

  public static PermissionRequest createDenyPermissionRequest(){
    return new PermissionRequest("DENY");
  }

}
