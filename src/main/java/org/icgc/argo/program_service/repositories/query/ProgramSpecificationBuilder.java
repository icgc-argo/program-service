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

package org.icgc.argo.program_service.repositories.query;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;

import javax.persistence.criteria.Root;
import java.util.UUID;

import static javax.persistence.criteria.JoinType.LEFT;

@Setter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ProgramSpecificationBuilder extends AbstractSpecificationBuilder<ProgramEntity, UUID> {

  private boolean fetchCancers;

  private boolean fetchPrimarySites;

  @Override
  protected Root<ProgramEntity> setupFetchStrategy(Root<ProgramEntity> root) {
    if (fetchCancers){
      root.fetch(ProgramEntity.Fields.programCancers, LEFT)
          .fetch(ProgramCancer.Fields.cancer, LEFT);
    }
    if (fetchPrimarySites){
      root.fetch(ProgramEntity.Fields.programPrimarySites, LEFT)
          .fetch(ProgramPrimarySite.Fields.primarySite, LEFT);
    }
    return root;
  }
}
