/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
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
  public static final String PROGRAMID_JOIN = "program_id";
  public static final String CANCERID_JOIN = "cancer_id";
  public static final String SITEID_JOIN = "primary_site_id";
  public static final String COUNTRYID_JOIN = "country_id";
  public static final String INSTITUTIONID_JOIN = "institution_id";
  public static final String REGIONID_JOIN = "region_id";
  public static final String LEGACY_SHORTNAME = "legacy_short_name";
  public static final String IS_ACTIVE = "active";
  public static final String ORGANIZATION = "organization";
  public static final String EMAIL = "email";
  public static final String UI_URL = "ui_url";
  public static final String GATEWAY_URL = "gateway_url";
  public static final String ANALYSIS_SONG_CODE = "analysis_song_code";
  public static final String ANALYSIS_SONG_URL = "analysis_song_url";
  public static final String ANALYSIS_SCORE_URL = "analysis_score_url";
  public static final String SUBMISSION_SONG_CODE = "submission_song_code";
  public static final String SUBMISSION_SONG_URL = "submission_song_url";
  public static final String SUBMISSION_SCORE_URL = "submission_score_url";
  public static final String DATA_CENTER_ID = "data_center_id";
}
