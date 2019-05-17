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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.icgc.argo.program_service.association.AssociatorException.checkConflict;
import static org.icgc.argo.program_service.association.AssociatorException.checkNotFound;
import static org.icgc.argo.program_service.utils.CollectionUtils.convertToIds;
import static org.icgc.argo.program_service.utils.CollectionUtils.difference;
import static org.icgc.argo.program_service.utils.Joiners.COMMA_SPACE;

/**
 * This class manages OneToMany BiDirectional relationships between a parent and its child entities.
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
    // check parent does not contain child
    val childrenFromParent = getChildrenFromParent(parent);
    val parentHasChildAlready = childrenFromParent.stream().anyMatch(x -> x.getId().equals(child.getId()));
    checkConflict(!parentHasChildAlready, "The %s with id '%s' already contains %s with id '%s'",
        parent.getClass().getSimpleName(), parent.getId().toString(),
        child.getClass().getSimpleName(), child.getId().toString());
    setParentFieldForChild(parent, child);
    return parent;
  }

  //TODO: [rtisma] check that the parent contains all the child ids
  @Override
  public P disassociate(P parent, Collection<CID> childIdsToDisassociate){
    val children = getChildrenFromParent(parent);
    val existingChildIds = convertToIds(children);
    checkMissingChildIds(parent, childIdsToDisassociate, existingChildIds);
    disassociateSelectedChildren(children, childIdsToDisassociate);
    return parent;
  }

  //TODO: [rtisma] check that all the parents contains all the child ids
  @Override
  public Collection<P> disassociate(Collection<P> parents, Collection<CID> childIdsToDisassociate){
    val parentToChildIdsMap = parents.stream()
        .collect(toMap(identity(), x -> convertToIds(getChildrenFromParent(x))));
    parentToChildIdsMap.forEach((parent, existingChildIds) ->
      checkMissingChildIds(parent, childIdsToDisassociate, existingChildIds)
    );
    parents.stream()
        .map(this::getChildrenFromParent)
        .forEach(children ->  disassociateSelectedChildren(children, childIdsToDisassociate));
    return parents;
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

  private void disassociateSelectedChildren(Collection<C> children, Collection<CID> selectedChildIds ){
    children.stream()
        .filter(x -> selectedChildIds.contains(x.getId()))
        .forEach(x -> setParentFieldForChild( null, x) );
    children.removeIf(x -> selectedChildIds.contains(x.getId()));
  }

  // Although stateless, left as method to reuse type boundaries. Could be static
  private void checkMissingChildIds(P parent, Collection<CID> childIdsToDisassociate, Collection<CID> existingChildIds){
    val nonExistingChildIds = difference(childIdsToDisassociate, existingChildIds);
    checkNotFound(
        nonExistingChildIds.isEmpty(),
        "The following child ids were not found for the %s with id '%s': %s",
        parent.getClass().getSimpleName(),
        parent.getId().toString(),
        "["+COMMA_SPACE.join(nonExistingChildIds)+"]");
  }


}
