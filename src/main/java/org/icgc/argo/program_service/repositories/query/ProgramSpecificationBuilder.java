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

package org.icgc.argo.program_service.repositories.query;

import static javax.persistence.criteria.JoinType.LEFT;

import java.util.UUID;
import javax.persistence.criteria.Root;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.*;

@Setter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ProgramSpecificationBuilder extends AbstractSpecificationBuilder<ProgramEntity, UUID> {

  private boolean fetchCancers;
  private boolean fetchPrimarySites;
  private boolean fetchInstitutions;
  private boolean fetchCountries;

  @Override
  protected Root<ProgramEntity> setupFetchStrategy(Root<ProgramEntity> root) {
    if (fetchCancers) {
      root.fetch(ProgramEntity.Fields.programCancers, LEFT)
          .fetch(ProgramCancer.Fields.cancer, LEFT);
    }
    if (fetchPrimarySites) {
      root.fetch(ProgramEntity.Fields.programPrimarySites, LEFT)
          .fetch(ProgramPrimarySite.Fields.primarySite, LEFT);
    }
    if (fetchInstitutions) {
      root.fetch(ProgramEntity.Fields.programInstitutions, LEFT)
          .fetch(ProgramInstitution.Fields.institution, LEFT);
    }
    if (fetchCountries) {
      root.fetch(ProgramEntity.Fields.programCountries, LEFT)
          .fetch(ProgramCountry.Fields.country, LEFT);
    }
    return root;
  }
}
