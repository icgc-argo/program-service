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

package org.icgc.argo.program_service.repositories.query;

import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;

public abstract class AbstractSpecificationBuilder<T, ID> {
  private static final String ID_FIELD = "id";
  private static final String NAME_FIELD = "name";

  protected abstract Root<T> setupFetchStrategy(Root<T> root);

  public Specification<T> buildByNameIgnoreCase(@NonNull String name) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsNameIgnoreCasePredicate(root, builder, name);
    };
  }

  public Specification<T> buildByShortName(@NonNull String shortName) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsNameShortNamePredicate(root, builder, shortName);
    };
  }

  public Specification<T> listAll(boolean distinct) {
    return (fromUser, query, builder) -> {
      query.distinct(distinct);
      val root = setupFetchStrategy(fromUser);
      return null;
    };
  }

  public Specification<T> buildById(@NonNull ID id) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsIdPredicate(root, builder, id);
    };
  }

  public Specification<T> buildByIds(@NonNull Collection<ID> ids) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return whereInIdsPredicate(root, ids);
    };
  }

  private Predicate whereInIdsPredicate(Root<T> root, Collection<ID> ids) {
    return root.get(ID_FIELD).in(ids);
  }

  private Predicate equalsIdPredicate(Root<T> root, CriteriaBuilder builder, ID id) {
    return builder.equal(root.get(ID_FIELD), id);
  }

  private Predicate equalsNameIgnoreCasePredicate(
      Root<T> root, CriteriaBuilder builder, String name) {
    return builder.equal(builder.upper(root.get(NAME_FIELD)), builder.upper(builder.literal(name)));
  }

  private Predicate equalsNameShortNamePredicate(
    Root<T> root, CriteriaBuilder builder, String shortName) {
    return builder.equal(root.get(ProgramEntity.Fields.shortName), builder.literal(shortName));
  }
}
