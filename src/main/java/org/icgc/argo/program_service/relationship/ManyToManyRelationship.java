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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;

import java.util.Collection;

import static java.util.stream.Collectors.toUnmodifiableSet;

/**
 * A ManyToMany relationship manager that manages relationships between 2 entities (P and C) using a join entity (J).
 * Since a ManyToMany can be represented as 2 OneToMany relationships, 2 OneToManyRelationship objects are required
 *
 * @param <P> parent entity type
 * @param <C> child entity type
 * @param <J> join entity type
 * @param <ID> parent and child ID type
 * @param <JID> join entity ID type
 */
@RequiredArgsConstructor
public abstract class ManyToManyRelationship<
    P extends IdentifiableEntity<ID>,
    C extends IdentifiableEntity<ID>,
    J extends IdentifiableEntity<JID> ,
    ID, JID> {

  @NonNull private final OneToManyRelationship<P, J, ID, JID> oneParentToManyJoinRelationship;
  @NonNull private final OneToManyRelationship<C, J, ID, JID> oneChildToManyJoinRelationship;

  /**
   * Associate the {@param parent} with the {@param children}
   */
  public P associate(P parent, Collection<C> children){
    children.forEach(child -> {
      val join = createJoinEntity(parent, child);
      oneParentToManyJoinRelationship.associate(parent, join);
      oneChildToManyJoinRelationship.associate(child, join);
    });
    return parent;
  }

  /**
   * Disassociate children matching ids contained in {@param childIdsToDisassociate} for the {@param parent}
   */
  public P disassociate(P parent, Collection<ID> childIdsToDisassociate){
    val parentJoinEntities = getJoinEntitiesFromParent(parent);

    val childEntities = parentJoinEntities.stream()
        .map(this::getChildFromJoinEntity)
        .filter(x -> childIdsToDisassociate.contains(x.getId()))
        .collect(toUnmodifiableSet());

    val commonJoinIds = parentJoinEntities.stream()
        .filter(x -> childIdsToDisassociate.contains(getChildFromJoinEntity(x).getId()))
        .map(IdentifiableEntity::getId)
        .collect(toUnmodifiableSet());

    oneParentToManyJoinRelationship.disassociate(parent, commonJoinIds);
    oneChildToManyJoinRelationship.disassociate(childEntities, commonJoinIds);
    return parent;
  }

  /**
   * Create a join entity (J) using the parent (P) and child (C)
   */
  protected abstract J createJoinEntity(P parent, C child);

  /**
   * Extract the join entities (J) from the parent entity (P)
   */
  protected abstract Collection<J> getJoinEntitiesFromParent(P parent);

  /**
   * Extract the child entity (C) from the join entity (J)
   */
  protected abstract C getChildFromJoinEntity(J joinEntity);

}
