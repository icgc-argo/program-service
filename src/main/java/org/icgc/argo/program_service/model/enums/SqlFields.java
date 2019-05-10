package org.icgc.argo.program_service.model.enums;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SqlFields {

  public static final String ID = "id";
  public static final String SHORTNAME = "short_name";
  public static final String NAME = "name";
  public static final String DESCRIPTION = "description";
  public static final String MEMBERSHIPTYPE = "membership_type";
  public static final String COMMITMENTDONORS = "commitment_donors";
  public static final String SUBMITTEDDONORS = "submitted_donors";
  public static final String GENOMICDONORS = "genomic_donors";
  public static final String WEBSITE = "website";
  public static final String CREATEDAT = "created_at";
  public static final String UPDATEDAT = "updated_at";
  public static final String INSTITUTIONS = "institutions";
  public static final String REGIONS = "regions";
  public static final String COUNTRIES = "countries";
  public static final String PROGRAMID_JOIN = "program_id";
  public static final String CANCER_TYPES = "cancer_types";
  public static final String SITEID_JOIN = "primary_site_id";

}
