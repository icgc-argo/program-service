/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
  public static final String CANCERID_JOIN = "cancer_id";
  public static final String SITEID_JOIN = "primary_site_id";

}
