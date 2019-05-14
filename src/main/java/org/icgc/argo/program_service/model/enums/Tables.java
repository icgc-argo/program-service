package org.icgc.argo.program_service.model.enums;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Tables {

  public static final String PROGRAM = "program";
  public static final String CANCER = "cancer";
  public static final String SITE = "primary_site";
  public static final String PROGRAM_CANCER = "program_cancer";
  public static final String PROGRAM_PRIMARY_SITE = "program_primary_site";
  public static final String PROGRAM_EGO_GROUP = "program_ego_group";

}
