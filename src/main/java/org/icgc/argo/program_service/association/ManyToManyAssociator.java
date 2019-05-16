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
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * A ManyToMany BiDirectional relationship manager that manages relationships between 2 entities (P and C) using a join entity (J).
 * Since a ManyToMany can be represented as 2 OneToMany relationships, 2 OneToManyRelationship objects are required
 *
 * @param <P> parent entity type
 * @param <C> child entity type
 * @param <J> join entity type
 * @param <ID> parent and child ID type
 * @param <JID> join entity ID type
 */

@Builder
@RequiredArgsConstructor
public class ManyToManyAssociator<
    P extends IdentifiableEntity<ID>,
    C extends IdentifiableEntity<ID>,
    J extends IdentifiableEntity<JID> ,
    ID, JID> implements Associator<P, C, ID> {

  /**
   * Dependencies
   */
  @NonNull private final OneToManyAssociator<P, J, ID, JID> oneParentToManyJoinRelationship;
  @NonNull private final OneToManyAssociator<C, J, ID, JID> oneChildToManyJoinRelationship;

  /**
   * Functions
   */
  @NonNull private final BiFunction<P, C, J> createJoinEntityFunction;
  @NonNull private final Function<P, Collection<J>> getJoinEntitiesFromParentFunction;
  @NonNull private final Function<J, C> getChildFromJoinEntityFunction;

  //TODO: [rtisma] check parent does not already have those children
  @Override
  public P associate(P parent, C child) {
    val join = createJoinEntity(parent, child);
    oneParentToManyJoinRelationship.associate(parent, join);
    oneChildToManyJoinRelationship.associate(child, join);
    return parent;
  }

  @Override
  public Collection<P> disassociate(Collection<P> parents, Collection<ID> childIdsToDisassociate) {
    parents.forEach(p -> disassociate(p, childIdsToDisassociate));
    return parents;
  }

  /**
   * Disassociate children matching ids contained in {@param childIdsToDisassociate} for the {@param parent}
   */
  //TODO: [rtisma] check parent has all the children from the id list
  @Override
  public P disassociate(P parent, Collection<ID> childIdsToDisassociate){
    val parentJoinEntities = getJoinEntitiesFromParent(parent);

    val jidToChildMap = parentJoinEntities.stream()
        .filter(x -> childIdsToDisassociate.contains(getChildFromJoinEntity(x).getId()))
        .collect(toMap(IdentifiableEntity::getId, this::getChildFromJoinEntity));

    val childEntities = jidToChildMap.values();
    val commonJoinIds = jidToChildMap.keySet();

    oneParentToManyJoinRelationship.disassociate(parent, commonJoinIds);
    oneChildToManyJoinRelationship.disassociate(childEntities, commonJoinIds);
    return parent;
  }

  /**
   * Create a join entity (J) using the parent (P) and child (C)
   */
  protected J createJoinEntity(P parent, C child){
    return createJoinEntityFunction.apply(parent, child);
  }

  /**
   * Extract the join entities (J) from the parent entity (P)
   */
  protected Collection<J> getJoinEntitiesFromParent(@NonNull P parent) {
    return getJoinEntitiesFromParentFunction.apply(parent);
  }

  /**
   * Extract the child entity (C) from the join entity (J)
   */
  protected C getChildFromJoinEntity(@NonNull J joinEntity) {
    return getChildFromJoinEntityFunction.apply(joinEntity);
  }

}
