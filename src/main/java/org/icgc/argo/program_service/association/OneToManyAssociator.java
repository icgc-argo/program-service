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

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

@Builder
@RequiredArgsConstructor
public class OneToManyAssociator<
    P extends IdentifiableEntity<PID>,
    C extends IdentifiableEntity<CID>,
    PID, CID> implements Associator<P, C, CID> {

  /**
   * Functions
   */
  @NonNull private final Function<P, Collection<C>> getChildrenFromParentFunction;
  @NonNull private final BiConsumer<P, C> setParentFieldForChildFunction;

  //TODO: [rtisma] check that the parent doesnt already have the child
  @Override
  public P associate(P parent, C child){
    getChildrenFromParent(parent).add(child);
    setParentFieldForChild(parent, child);
    return parent;
  }

  //TODO: [rtisma] check that the parent contains all the child ids
  @Override
  public P disassociate(P parent, Collection<CID> childIdsToDisassociate){
    val children = getChildrenFromParent(parent);
    disassociateSelectedChildren(children, childIdsToDisassociate);
    return parent;
  }

  //TODO: [rtisma] check that all the parents contains all the child ids
  @Override
  public Collection<P> disassociate(Collection<P> parents, Collection<CID> childIdsToDisassociate){
    parents.stream()
        .map(this::getChildrenFromParent)
        .forEach(children ->  disassociateSelectedChildren(children, childIdsToDisassociate));
    return parents;
  }

  private void disassociateSelectedChildren(Collection<C> children, Collection<CID> selectedChildIds ){
    children.stream()
        .filter(x -> selectedChildIds.contains(x.getId()))
        .forEach(x -> setParentFieldForChild( null, x) );
    children.removeIf(x -> selectedChildIds.contains(x.getId()));
  }

  /**
   * Extract the children from the {@param parent}
   */
  public Collection<C> getChildrenFromParent(@NonNull P parent) {
    return getChildrenFromParentFunction.apply(parent);
  }

  /**
   * Set the {@param parent} with the {@param child}
   */
  protected void setParentFieldForChild(P parent, @NonNull C child) {
    setParentFieldForChildFunction.accept(parent, child);
  }

}
