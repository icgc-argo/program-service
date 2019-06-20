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

import lombok.NoArgsConstructor;

import javax.print.DocFlavor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Tables {
  public static final String PROGRAM = "program";
  public static final String CANCER = "cancer";
  public static final String SITE = "primary_site";
  public static final String PROGRAM_CANCER = "program_cancer";
  public static final String PROGRAM_PRIMARY_SITE = "program_primary_site";
  public static final String PROGRAM_EGO_GROUP = "program_ego_group";
  public static final String JOIN_PROGRAM_INVITE = "join_program_invite";

}
