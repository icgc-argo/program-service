package org.icgc.argo.program_service.utils;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.repositories.BaseRepository;
import java.util.Collection;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;
import static org.icgc.argo.program_service.model.exceptions.NotFoundException.*;
import static org.icgc.argo.program_service.utils.Joiners.*;

@NoArgsConstructor(access = PRIVATE)
public class EntityService {

  public static <T extends IdentifiableEntity<ID>, ID> Set<T> getManyEntities(
          @NonNull Class<T> entityType,
          @NonNull BaseRepository<T, ID> repository,
          @NonNull Collection<ID> ids) {
    val entities = repository.findAllByIdIn(ImmutableList.copyOf(ids));

    val requestedIds = ImmutableSet.copyOf(ids);
    val existingIds = entities.stream().map(IdentifiableEntity::getId).collect(toImmutableSet());
    val nonExistingIds = difference(requestedIds, existingIds);

    checkNotFound(
            nonExistingIds.isEmpty(),
            "Entities of entityType '%s' were not found for the following ids: %s",
            resolveEntityTypeName(entityType),
            COMMA.join(nonExistingIds));
    return entities;
  }

  private static String resolveEntityTypeName(Class<?> entityType) {
    return entityType.getSimpleName();
  }

}
