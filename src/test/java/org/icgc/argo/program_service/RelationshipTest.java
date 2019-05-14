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

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang.NotImplementedException;

public class RelationshipTest {

  // one2ManyAssociate_unassociatedChildren_Success
  // one2ManyAssociate_someChildrenAlreadyAssociated_Conflict
  // one2ManyDisassociate_allChildrenAssociated_Success
  // one2ManyDisassociate_someChildrenNotAssociated_NotFound

  // many2ManyAssociate_unassociatedChildren_Success
  // many2ManyAssociate_someChildrenAlreadyAssociated_Conflict
  // many2ManyDisassociate_allChildrenAssociated_Success
  // many2ManyDisassociate_someChildrenNotAssociated_NotFound

	@Test
	public void one2ManyAssociate_unassociatedChildren_Success(){
		throw new NotImplementedException("the test one2ManyAssociate_unassociatedChildren_Success is not implemented yet");
	}


	@Test
	public void one2ManyAssociate_someChildrenAlreadyAssociated_Conflict(){
		throw new NotImplementedException("the test one2ManyAssociate_someChildrenAlreadyAssociated_Conflict is not implemented yet");
	}


	@Test
	public void one2ManyDisassociate_allChildrenAssociated_Success(){
		throw new NotImplementedException("the test one2ManyDisassociate_allChildrenAssociated_Success is not implemented yet");
	}


	@Test
	public void one2ManyDisassociate_someChildrenNotAssociated_NotFound(){
		throw new NotImplementedException("the test one2ManyDisassociate_someChildrenNotAssociated_NotFound is not implemented yet");
	}


	@Test
	public void many2ManyAssociate_unassociatedChildren_Success(){
		throw new NotImplementedException("the test many2ManyAssociate_unassociatedChildren_Success is not implemented yet");
	}


	@Test
	public void many2ManyAssociate_someChildrenAlreadyAssociated_Conflict(){
		throw new NotImplementedException("the test many2ManyAssociate_someChildrenAlreadyAssociated_Conflict is not implemented yet");
	}


	@Test
	public void many2ManyDisassociate_allChildrenAssociated_Success(){
		throw new NotImplementedException("the test many2ManyDisassociate_allChildrenAssociated_Success is not implemented yet");
	}


	@Test
	public void many2ManyDisassociate_someChildrenNotAssociated_NotFound(){
		throw new NotImplementedException("the test many2ManyDisassociate_someChildrenNotAssociated_NotFound is not implemented yet");
	}



}
