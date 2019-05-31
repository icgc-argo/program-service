package org.icgc.argo.program_service.utils;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;

@NoArgsConstructor(access = PRIVATE)
public class EntityService {

  public static <T extends IdentifiableEntity<ID>, ID> Set<T> getManyEntities(
          @NonNull Class<T> entityType,
          @NonNull JpaRepository<T, ID> repository,
          @NonNull Collection<ID> ids) {
    val entities = repository.findAllById(ImmutableList.copyOf(ids));
    val entitySet = entities.stream().collect(toImmutableSet());
    val requestedIds = ImmutableSet.copyOf(ids);
    val existingIds = entities.stream().map(IdentifiableEntity::getId).collect(toImmutableSet());
    val nonExistingIds = difference(requestedIds, existingIds);

    if(!nonExistingIds.isEmpty()) {
      throw new EmptyResultDataAccessException(
              String.format("Entities of entityType '%s' were not found for the following ids: '%s' .",
                      resolveEntityTypeName(entityType), requestedIds),
              requestedIds.size());
    }

    return entitySet;
  }

  private static String resolveEntityTypeName(Class<?> entityType) {
    return entityType.getSimpleName();
  }

}
