package org.icgc.argo.program_service.utils;


import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;
import org.icgc.argo.program_service.model.entity.NameableEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.repositories.BaseRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;

@Slf4j
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
                      resolveEntityTypeName(entityType), nonExistingIds),
              requestedIds.size());
    }

    return entitySet;
  }

  public static <T extends NameableEntity<ID>, ID> List<T> checkExistenceByName(
          @NonNull Class<T> entityType,
          @NonNull BaseRepository<T, ID> repository,
          @NonNull List<String> names){

    val entities = repository.findAllByNameIn(names);
    val requestedNames = ImmutableSet.copyOf(names);
    val existingNames = mapToSet(entities, NameableEntity::getName);
    val nonExistingNames = CollectionUtils.difference(requestedNames, existingNames);

    if(!nonExistingNames.isEmpty()) {
      val msg = String.format("The following %s names do not exist: %s",
              resolveEntityTypeName(entityType),
              Joiner.on(" , ").join(nonExistingNames));
      log.error(msg);
      throw new NotFoundException(msg);
    }
    return entities;
  }

  public static <T extends NameableEntity<ID>, ID> List<T> checkDuplicate(
          @NonNull Class<T> entityType,
          @NonNull BaseRepository<T, ID> repository,
          @NonNull List<String> names){

    val entities = repository.findAllByNameIn(names);
    val requestedNames = ImmutableSet.copyOf(names);
    val existingNames = mapToSet(entities, NameableEntity::getName);
    val duplicateNames = CollectionUtils.intersection(requestedNames, existingNames);

    if(!duplicateNames.isEmpty()) {
      val msg = String.format("The following %s names already exist: %s",
              resolveEntityTypeName(entityType),
              Joiner.on(" , ").join(duplicateNames));
      throw new DuplicateKeyException(msg);
    }
    return entities;
  }

  public static void checkEmpty(@NonNull List<String> names){
    if(names.isEmpty() || names.contains("")) throw new DataIntegrityViolationException("Entity name cannot be empty.");
  }

  private static String resolveEntityTypeName(Class<?> entityType) {
    return entityType.getSimpleName();
  }

}
