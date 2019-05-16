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

package org.icgc.argo.program_service.association;

import org.icgc.argo.program_service.model.entity.IdentifiableEntity;

import java.util.Collection;

public interface Associator<P, C extends IdentifiableEntity<CID>, CID>{

  /**
   * Associate the {@param child} with the {@param parent}
   */
  P associate(P parent, C child);

  /**
   * Disassociate children matching ids contained in {@param childIdsToDisassociate} from the input {@param parent}
   */
  P disassociate(P parent, Collection<CID> childIdsToDisassociate);

  /**
   * Disassociate children matching ids contained in {@param childIdsToDisassociate} from all input {@param parents}
   */
  Collection<P> disassociate(Collection<P> parents, Collection<CID> childIdsToDisassociate);

  /**
   * Associate the {@param children} with the {@param parent}
   */
  default P associate( P parent, Collection<C> children){
    children.forEach(x -> associate(parent, x));
    return parent;
  }


}
