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

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Factory that creates OneToMany and ManyToMany relationship objects for managing relationships
 * between a parent and child type using Lambda functions.
 *
 * @param <P> parent entity type
 * @param <C> child entity type
 * @param <J> join entity type
 * @param <ID> parent and child ID type
 * @param <JID> join entity ID type
 */
@Value
@Builder
public class AssociatorFactory<
      P extends IdentifiableEntity<ID>,
      C extends IdentifiableEntity<ID>,
      J extends IdentifiableEntity<JID> ,
      ID, JID>  {

  @NonNull private final BiFunction<P, C, J> createJoinEntityFunction;
  @NonNull private final Function<P, Collection<J>> getJoinEntitiesFromParentFunction;
  @NonNull private final Function<C, Collection<J>> getJoinEntitiesFromChildFunction;
  @NonNull private final Function<J, C> getChildFromJoinEntityFunction;
  @NonNull private final BiConsumer<P, J> setParentForJoinEntityFunction;
  @NonNull private final BiConsumer<C, J> setChildForJoinEntityFunction;

  public AbstractOneToManyAssociator<P, J, ID, JID> buildParentOneToManyRelationship(){
    return new AbstractOneToManyAssociator<>(){

      @Override public Collection<J> getChildrenFromParent(@NonNull P parent) {
        return getJoinEntitiesFromParentFunction.apply(parent);
      }

      @Override protected void setParentForChild(@NonNull J joinEntity, P parent) {
        setParentForJoinEntityFunction.accept(parent, joinEntity);
      }
    };
  }

  public AbstractOneToManyAssociator<C, J, ID, JID> buildChildOneToManyRelationship(){
    return new AbstractOneToManyAssociator<>(){

      @Override public Collection<J> getChildrenFromParent(@NonNull C child) {
        return getJoinEntitiesFromChildFunction.apply(child);
      }

      @Override protected void setParentForChild(@NonNull J joinEntity, C child) {
        setChildForJoinEntityFunction.accept(child, joinEntity);
      }
    };
  }

  public AbstractManyToManyAssociator<P, C, J, ID, JID> buildManyToManyRelationship(){
    val left = buildParentOneToManyRelationship();
    val right = buildChildOneToManyRelationship();
    return new AbstractManyToManyAssociator<>(left, right) {

      @Override protected J createJoinEntity(@NonNull P parent, @NonNull C child) {
        return createJoinEntityFunction.apply(parent, child);
      }

      @Override protected Collection<J> getJoinEntitiesFromParent(@NonNull P parent) {
        return getJoinEntitiesFromParentFunction.apply(parent);
      }

      @Override protected C getChildFromJoinEntity(@NonNull J joinEntity) {
        return getChildFromJoinEntityFunction.apply(joinEntity);
      }
    };

  }
}
