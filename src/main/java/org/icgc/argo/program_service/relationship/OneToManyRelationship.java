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

import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;

import java.util.Collection;

/**
 * This abstract class manages relationships between a parent and its child entities in a OneToMany relationship.
 * Each parent and child type pair will have a specific implementation, which is why this class is abstract.
 * The purpose of this class is to ensure a consistent way of managing bidirectional relationships between
 * entities that have a OneToMany relationship.
 *
 * @param <P> parent entity type
 * @param <C> child entity type
 * @param <PID> parent entity ID type
 * @param <CID> child entity ID type
 */
//TODO: [rtisma] replace this with a concrete Lamba implementation, of the interface OneToManyRelationship. The interface should not be bounded by IdentifiableEntity
public abstract class OneToManyRelationship<
    P extends IdentifiableEntity<PID>,
    C extends IdentifiableEntity<CID>,
    PID, CID> {

  /**
   * Associate the {@param child} with the {@param parent}
   */
  //TODO: [rtisma] check that the parent doesnt already have the child
  public P associate(P parent, C child){
    getChildrenFromParent(parent).add(child);
    setParentForChild(child, parent);
    return parent;
  }

  /**
   * Associate the {@param children} with the {@param parent}
   */
  //TODO: [rtisma] check that the parent doesnt already have the children
  public P associate(P parent, Collection<C> children){
    children.forEach(x -> associate(parent, x));
    return parent;
  }

  /**
   * Disassociate children matching ids contained in {@param childIdsToDisassociate} from the input {@param parent}
   */
  //TODO: [rtisma] check that the parent contains all the child ids
  public P disassociate(P parent, Collection<CID> childIdsToDisassociate){
    val children = getChildrenFromParent(parent);
    disassociateSelectedChildren(children, childIdsToDisassociate);
    return parent;
  }

  /**
   * Disassociate children matching ids contained in {@param childIdsToDisassociate} from all input {@param parents}
   */
  //TODO: [rtisma] check that all the parents contains all the child ids
  public Collection<P> disassociate(Collection<P> parents, Collection<CID> childIdsToDisassociate){
    parents.stream()
        .map(this::getChildrenFromParent)
        .forEach(children ->  disassociateSelectedChildren(children, childIdsToDisassociate));
    return parents;
  }

  /**
   * Disassociate children matching the id {@param childIdsToDisassociate} from all input {@param parents}
   */
  //TODO: [rtisma] check that all the parents contain the child id
  public Collection<P> disassociate(Collection<P> parents, CID childIdToDisassociate){
    return disassociate(parents, ImmutableSet.of(childIdToDisassociate));
  }

  private void disassociateSelectedChildren(Collection<C> children, Collection<CID> selectedChildIds ){
    children.stream()
        .filter(x -> selectedChildIds.contains(x.getId()))
        .forEach(x -> setParentForChild(x, null) );
    children.removeIf(x -> selectedChildIds.contains(x.getId()));
  }

  /**
   * Extract the children from the {@param parent}
   */
  public abstract Collection<C> getChildrenFromParent(P parent);

  /**
   * Set the {@param parent} with the {@param child}
   */
  protected abstract void setParentForChild(C child, P parent);

}