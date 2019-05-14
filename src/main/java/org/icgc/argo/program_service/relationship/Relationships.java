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

package org.icgc.argo.program_service.relationship;

import lombok.NoArgsConstructor;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramCancerId;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.icgc.argo.program_service.model.join.ProgramPrimarySiteId;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Relationships {

  public static final ManyToManyRelationship<ProgramEntity, CancerEntity, ProgramCancer, UUID, ProgramCancerId>
      PROGRAM_CANCER_RELATIONSHIP = ManyToManyRelationshipFactory.<ProgramEntity, CancerEntity,
      ProgramCancer, UUID, ProgramCancerId>builder()
      .createJoinEntityFunction(ProgramCancer::createProgramCancer)
      .getJoinEntitiesFromChildFunction(CancerEntity::getProgramCancers)
      .getJoinEntitiesFromParentFunction(ProgramEntity::getProgramCancers)
      .getChildFromJoinEntityFunction(ProgramCancer::getCancer)
      .setChildForJoinEntityFunction((cancer, programCancer) -> programCancer.setCancer(cancer))
      .setParentForJoinEntityFunction((program, programCancer) -> programCancer.setProgram(program))
      .build()
      .buildManyToManyRelationship();

  public static final ManyToManyRelationship<ProgramEntity, PrimarySiteEntity,
      ProgramPrimarySite, UUID, ProgramPrimarySiteId> PROGRAM_PRIMARY_SITE_RELATIONSHIP =
      ManyToManyRelationshipFactory.<ProgramEntity, PrimarySiteEntity,
      ProgramPrimarySite, UUID, ProgramPrimarySiteId>builder()
      .createJoinEntityFunction(ProgramPrimarySite::createProgramPrimarySite)
      .getJoinEntitiesFromChildFunction(PrimarySiteEntity::getProgramPrimarySites)
          .getJoinEntitiesFromParentFunction(ProgramEntity::getProgramPrimarySites)
      .getChildFromJoinEntityFunction(ProgramPrimarySite::getPrimarySite)
      .setChildForJoinEntityFunction((primarySite, programPrimarySite) -> programPrimarySite.setPrimarySite(primarySite))
      .setParentForJoinEntityFunction((primarySite, programPrimarySite) -> programPrimarySite.setProgram(primarySite))
      .build()
      .buildManyToManyRelationship();

}
