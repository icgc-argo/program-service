package org.icgc.argo.program_service.repositories.query;

import lombok.NonNull;
import lombok.val;
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

  public Specification<T> listAll() {
    return (fromUser, query, builder) -> {
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
}
