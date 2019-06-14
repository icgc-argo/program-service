package org.icgc.argo.program_service.services.ego;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.icgc.argo.program_service.proto.UserRole;

import static java.lang.String.format;

@Value
@Builder
public class GroupName {

  private static final String FORMAT = "%s-%s-%s";

  @NonNull private final String contextName;
  @NonNull private final String programShortName;
  @NonNull private final UserRole role;

  @Override
  public String toString() {
    return format(FORMAT, contextName, programShortName, role.name());
  }

  public static GroupName createProgramGroupName(String name, UserRole role){
    return GroupName.builder()
        .contextName("PROGRAM")
        .programShortName(name)
        .role(role)
        .build();
  }
}
