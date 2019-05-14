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

package org.icgc.argo.program_service;

import lombok.val;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;
import org.testcontainers.shaded.org.apache.commons.lang.NotImplementedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.RandomGenerator.createRandomGenerator;
import static org.icgc.argo.program_service.relationship.Relationships.PROGRAM_CANCER_RELATIONSHIP;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToImmutableList;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToList;
import static org.icgc.argo.program_service.utils.CollectionUtils.repeatedCallsOf;

public class RelationshipTest {

  // one2ManyAssociate_unassociatedChildren_Success
  // one2ManyAssociate_someChildrenAlreadyAssociated_Conflict
  // one2ManyDisassociate_allChildrenAssociated_Success
  // one2ManyDisassociate_someChildrenNotAssociated_NotFound

  // many2ManyAssociate_unassociatedChildren_Success
  // many2ManyAssociate_someChildrenAlreadyAssociated_Conflict
  // many2ManyDisassociate_allChildrenAssociated_Success
  // many2ManyDisassociate_someChildrenNotAssociated_NotFound

	private RandomGenerator randomGenerator;
	private List<ProgramEntity> programs;
	private List<CancerEntity> cancers;

	@BeforeEach
	public void beforeTest(){
		this.randomGenerator = createRandomGenerator("relationship-test");
		this.programs = repeatedCallsOf(this::generateRandomProgramEntity, 3);
		this.cancers = repeatedCallsOf(this::generateRandomCancerEntity, 3);
	}

	private ProgramEntity generateRandomProgramEntity(){
	  return new ProgramEntity()
				.setName(randomGenerator.randomAsciiString(7))
				.setId(randomGenerator.randomUUID())
				.setMembershipType(randomGenerator.randomEnumOf(MembershipType.class))
				.setSubmittedDonors(randomGenerator.randomUnboundedInt());
	}

	private CancerEntity generateRandomCancerEntity(){
		return new CancerEntity()
				.setId(randomGenerator.randomUUID())
				.setName(randomGenerator.randomAsciiString(8));
	}

	@Test
	public void one2ManyAssociate_unassociatedChildren_Success(){
		// Assert all programs are not associated with any cancers
	  programs.forEach(x -> assertThat(x.getProgramCancers()).isEmpty());
	  cancers.forEach(x -> assertThat(x.getProgramCancers()).isEmpty());

	  // Create relationships
		PROGRAM_CANCER_RELATIONSHIP.associate(programs.get(0), cancers);
		PROGRAM_CANCER_RELATIONSHIP.associate(programs.get(1), cancers.subList(0,1));
		PROGRAM_CANCER_RELATIONSHIP.associate(programs.get(2), cancers.subList(0,2));

		// Assert first program has all 3 cancers
		assertThat(programs.get(0).getProgramCancers()).hasSize(3);
		assertThat(mapToList(programs.get(0).getProgramCancers(), ProgramCancer::getCancer))
				.containsExactlyInAnyOrderElementsOf(cancers);

		// Assert second program has the first cancer
		assertThat(programs.get(1).getProgramCancers()).hasSize(1);
		assertThat(mapToList(programs.get(1).getProgramCancers(), ProgramCancer::getCancer))
				.containsExactlyInAnyOrderElementsOf(cancers.subList(0,1));

		// Assert third program has the first 2 cancers
		assertThat(programs.get(2).getProgramCancers()).hasSize(2);
		assertThat(mapToList(programs.get(2).getProgramCancers(), ProgramCancer::getCancer))
				.containsExactlyInAnyOrderElementsOf(cancers.subList(0,2));

		// Assert first cancer has 3 programs
		assertThat(cancers.get(0).getProgramCancers()).hasSize(3);
		assertThat(mapToList(cancers.get(0).getProgramCancers(), ProgramCancer::getProgram))
				.containsExactlyInAnyOrderElementsOf(programs);

		// Assert second cancer has first and third programs
		assertThat(cancers.get(1).getProgramCancers()).hasSize(2);
		assertThat(mapToList(cancers.get(1).getProgramCancers(), ProgramCancer::getProgram))
				.containsExactlyInAnyOrderElementsOf(ImmutableList.of(programs.get(0), programs.get(2)));

		// Assert third cancer has only the first program
		assertThat(cancers.get(2).getProgramCancers()).hasSize(1);
		assertThat(mapToList(cancers.get(2).getProgramCancers(), ProgramCancer::getProgram))
				.containsExactlyInAnyOrderElementsOf(ImmutableList.of(programs.get(0)));
	}

	@Test
	@Ignore
	public void one2ManyAssociate_someChildrenAlreadyAssociated_Conflict(){
		throw new NotImplementedException("the test one2ManyAssociate_someChildrenAlreadyAssociated_Conflict is not implemented yet");
	}


	@Test
	public void one2ManyDisassociate_allChildrenAssociated_Success(){
		// Assert all programs are not associated with any cancers, and vice-versa
		programs.forEach(x -> assertThat(x.getProgramCancers()).isEmpty());
		cancers.forEach(x -> assertThat(x.getProgramCancers()).isEmpty());

		// Create relationships
		PROGRAM_CANCER_RELATIONSHIP.associate(programs.get(0), cancers); // Cancer 0, 1 and 2
		PROGRAM_CANCER_RELATIONSHIP.associate(programs.get(1), cancers.subList(0,1)); // Cancer 0
		PROGRAM_CANCER_RELATIONSHIP.associate(programs.get(2), cancers.subList(0,2)); // Cancer 0 and 1

		// Disassociate relationships
		val cancerIds = mapToImmutableList(cancers, IdentifiableEntity::getId);
		val programIds = mapToImmutableList(programs, IdentifiableEntity::getId);

		// Disassociate the Cancer 0 and 1 for Program 0
		PROGRAM_CANCER_RELATIONSHIP.disassociate(programs.get(0), cancerIds.subList(0,2));

		// Assert only 1 left for Program 0 (i.e Cancer 2)
		assertThat(programs.get(0).getProgramCancers()).hasSize(1);
		val actualCancerIds = mapToImmutableList(programs.get(0).getProgramCancers(), x -> x.getCancer().getId());
		assertThat(actualCancerIds).containsExactlyInAnyOrderElementsOf(cancerIds.subList(2,3));

		// Assert Cancer 0 has Program 1 and 2
    assertThat(cancers.get(0).getProgramCancers()).hasSize(2);
    val actualProgramIds = mapToImmutableList(cancers.get(0).getProgramCancers(), x -> x.getProgram().getId());
    assertThat(actualProgramIds).containsExactlyInAnyOrderElementsOf(programIds.subList(1,3));

    // Assert Cancer 1 has Program 2
		assertThat(cancers.get(1).getProgramCancers()).hasSize(1);
		val actualProgramIds2 = mapToImmutableList(cancers.get(1).getProgramCancers(), x -> x.getProgram().getId());
		assertThat(actualProgramIds2).containsExactlyInAnyOrderElementsOf(programIds.subList(2,3));

		// Assert Cancer 2 has Program 0
		assertThat(cancers.get(2).getProgramCancers()).hasSize(1);
		val actualProgramIds3 = mapToImmutableList(cancers.get(2).getProgramCancers(), x -> x.getProgram().getId());
		assertThat(actualProgramIds3).containsExactlyInAnyOrderElementsOf(programIds.subList(0,1));

		// Disassociate the Cancer 2 from Program 0
		PROGRAM_CANCER_RELATIONSHIP.disassociate(programs.get(0), cancerIds.subList(2,3));

		// Assert Cancer 2 has no more programs
		assertThat(cancers.get(2).getProgramCancers()).isEmpty();

		// Assert Program 0 has no more associated cancers
		assertThat(programs.get(0).getProgramCancers()).isEmpty();

		// Disassociate all cancers from Program 1 and 2
		PROGRAM_CANCER_RELATIONSHIP.disassociate(programs.get(1), cancerIds.subList(0,1));
		PROGRAM_CANCER_RELATIONSHIP.disassociate(programs.get(2), cancerIds.subList(0,2));

		// Assert all programs do not have cancers associated do not have any associated cancers, and vice-versa
		programs.forEach(x -> assertThat(x.getProgramCancers()).isEmpty());
		cancers.forEach(x -> assertThat(x.getProgramCancers()).isEmpty());
	}


	@Test
	@Ignore
	public void one2ManyDisassociate_someChildrenNotAssociated_NotFound(){
		throw new NotImplementedException("the test one2ManyDisassociate_someChildrenNotAssociated_NotFound is not implemented yet");
	}


	@Test
	public void many2ManyAssociate_unassociatedChildren_Success(){
		throw new NotImplementedException("the test many2ManyAssociate_unassociatedChildren_Success is not implemented yet");
	}


	@Test
	@Ignore
	public void many2ManyAssociate_someChildrenAlreadyAssociated_Conflict(){
		throw new NotImplementedException("the test many2ManyAssociate_someChildrenAlreadyAssociated_Conflict is not implemented yet");
	}


	@Test
	public void many2ManyDisassociate_allChildrenAssociated_Success(){
		throw new NotImplementedException("the test many2ManyDisassociate_allChildrenAssociated_Success is not implemented yet");
	}


	@Test
	@Ignore
	public void many2ManyDisassociate_someChildrenNotAssociated_NotFound(){
		throw new NotImplementedException("the test many2ManyDisassociate_someChildrenNotAssociated_NotFound is not implemented yet");
	}



}
