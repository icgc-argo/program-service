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
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.icgc.argo.program_service.relationship.Associator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.NotImplementedException;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.argo.program_service.RandomGenerator.createRandomGenerator;
import static org.icgc.argo.program_service.relationship.Associators.PROGRAM_CANCER_ASSOCIATOR;
import static org.icgc.argo.program_service.relationship.Associators.PROGRAM_PRIMARY_SITE_ASSOCIATOR;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToImmutableList;
import static org.icgc.argo.program_service.utils.CollectionUtils.repeatedCallsOf;

public class AssociatorTest {

	private RandomGenerator randomGenerator;

	@BeforeEach
	public void beforeTest(){
		this.randomGenerator = createRandomGenerator("relationship-test");
	}

	/**
	 * Tests association and disassociation of ProgramEntity, ProgramCancer and CancerEntity
	 */
	@Test
	public void programCancerMany2ManyAssociation_unassociated_Success(){
		runMany2ManyAssociation_AllChildrenAssociated_SuccessTest(PROGRAM_CANCER_ASSOCIATOR,
				this::generateRandomProgramEntity,
				this::generateRandomCancerEntity,
				ProgramEntity::getProgramCancers,
				CancerEntity::getProgramCancers,
				ProgramCancer::getProgram,
				ProgramCancer::getCancer);

	}

	/**
	 * Tests association and disassociation of ProgramEntity, ProgramPrimarySite and PrimarySiteEntity
	 */
	@Test
	public void programPrimarySiteMany2ManyAssociation_unassociated_Success(){
		runMany2ManyAssociation_AllChildrenAssociated_SuccessTest(PROGRAM_PRIMARY_SITE_ASSOCIATOR,
				this::generateRandomProgramEntity,
				this::generateRandomPrimarySiteEntity,
				ProgramEntity::getProgramPrimarySites,
				PrimarySiteEntity::getProgramPrimarySites,
				ProgramPrimarySite::getProgram,
				ProgramPrimarySite::getPrimarySite);
	}

	@Test
	@Disabled
	public void one2ManyAssociate_someChildrenAlreadyAssociated_Conflict(){
		throw new NotImplementedException("the test one2ManyAssociate_someChildrenAlreadyAssociated_Conflict is not implemented yet");
	}

	@Test
	@Disabled
	public void one2ManyDisassociate_someChildrenNotAssociated_NotFound(){
		throw new NotImplementedException("the test one2ManyDisassociate_someChildrenNotAssociated_NotFound is not implemented yet");
	}

	@Test
	@Disabled
	public void many2ManyAssociate_someChildrenAlreadyAssociated_Conflict(){
		throw new NotImplementedException("the test many2ManyAssociate_someChildrenAlreadyAssociated_Conflict is not implemented yet");
	}


	@Test
	@Disabled
	public void many2ManyDisassociate_allChildrenAssociated_Success(){
		throw new NotImplementedException("the test many2ManyDisassociate_allChildrenAssociated_Success is not implemented yet");
	}


	@Test
	@Disabled
	public void many2ManyDisassociate_someChildrenNotAssociated_NotFound(){
		throw new NotImplementedException("the test many2ManyDisassociate_someChildrenNotAssociated_NotFound is not implemented yet");
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

	private PrimarySiteEntity generateRandomPrimarySiteEntity(){
		return new PrimarySiteEntity()
				.setId(randomGenerator.randomUUID())
				.setName(randomGenerator.randomAsciiString(9));
	}

	/**
	 * This test creates associations, asserts them, and then disassociates them and asserts them
	 */
	private static <P extends IdentifiableEntity<ID>, C extends IdentifiableEntity<ID>, J, ID>
	void runMany2ManyAssociation_AllChildrenAssociated_SuccessTest(
			Associator<P, C, ID> associator,
			Supplier<P> randomParentGeneratorFunction,
			Supplier<C> randomChildGeneratorFunction,
			Function<P, Collection<J>> getJoinEntitiesFromParentFunction,
			Function<C, Collection<J>> getJoinEntitiesFromChildFunction,
			Function<J, P> getParentFromJoinEntityFunction,
			Function<J, C> getChildFromJoinEntityFunction ){

		// Generate data
		val parents = repeatedCallsOf(randomParentGeneratorFunction, 3);
		val children = repeatedCallsOf(randomChildGeneratorFunction, 3);

		// Assert all parents are not associated with any children, and vice-versa
		parents.forEach(x -> assertThat(getJoinEntitiesFromParentFunction.apply(x)).isEmpty());
		children.forEach(x -> assertThat(getJoinEntitiesFromChildFunction.apply(x)).isEmpty());

		// Create relationships
		associator.associate(parents.get(0), children); // Child 0, 1 and 2
		associator.associate(parents.get(1), children.subList(0,1)); // Child 0
		associator.associate(parents.get(2), children.subList(0,2)); // Child 0 and 1

		// Disassociate relationships
		val childIds = mapToImmutableList(children, IdentifiableEntity::getId);
		val parentIds = mapToImmutableList(parents, IdentifiableEntity::getId);

		// Disassociate the Child 0 and 1 for Program 0
		associator.disassociate(parents.get(0), childIds.subList(0,2));

		// Assert only 1 left for Parent 0 (i.e Child 2)
		assertThat(getJoinEntitiesFromParentFunction.apply(parents.get(0))).hasSize(1);
		val actualChildIds = mapToImmutableList(getJoinEntitiesFromParentFunction.apply(parents.get(0)),
				x -> getChildFromJoinEntityFunction.apply(x).getId());
		assertThat(actualChildIds).containsExactlyInAnyOrderElementsOf(childIds.subList(2,3));

		// Assert Child 0 has Parent 1 and 2
		assertThat(getJoinEntitiesFromChildFunction.apply(children.get(0))).hasSize(2);
		val actualParentIds = mapToImmutableList(getJoinEntitiesFromChildFunction.apply(children.get(0)),
				x -> getParentFromJoinEntityFunction.apply(x).getId());
		assertThat(actualParentIds).containsExactlyInAnyOrderElementsOf(parentIds.subList(1,3));

		// Assert Child 1 has Parent 2
		assertThat(getJoinEntitiesFromChildFunction.apply(children.get(1))).hasSize(1);
		val actualParentIds2 = mapToImmutableList(getJoinEntitiesFromChildFunction.apply(children.get(1)),
				x -> getParentFromJoinEntityFunction.apply(x).getId());
		assertThat(actualParentIds2).containsExactlyInAnyOrderElementsOf(parentIds.subList(2,3));

		// Assert Child 2 has Parent 0
		assertThat(getJoinEntitiesFromChildFunction.apply(children.get(2))).hasSize(1);
		val actualParentIds3 = mapToImmutableList(getJoinEntitiesFromChildFunction.apply(children.get(2)),
				x -> getParentFromJoinEntityFunction.apply(x).getId());
		assertThat(actualParentIds3).containsExactlyInAnyOrderElementsOf(parentIds.subList(0,1));

		// Disassociate the Child 2 from Parent 0
		associator.disassociate(parents.get(0), childIds.subList(2,3));

		// Assert Child 2 has no more parents
		assertThat(getJoinEntitiesFromChildFunction.apply(children.get(2))).isEmpty();

		// Assert Parent 0 has no more associated children
		assertThat(getJoinEntitiesFromParentFunction.apply(parents.get(0))).isEmpty();

		// Disassociate all children from Parent 1 and 2
		associator.disassociate(parents.get(1), childIds.subList(0,1));
		associator.disassociate(parents.get(2), childIds.subList(0,2));

		// Assert all parents do not have children associated do not have any associated children, and vice-versa
		parents.forEach(x -> assertThat(getJoinEntitiesFromParentFunction.apply(x)).isEmpty());
		children.forEach(x -> assertThat(getJoinEntitiesFromChildFunction.apply(x)).isEmpty());
	}

}
