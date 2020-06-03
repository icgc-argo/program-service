/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.utils;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    if (!nonExistingIds.isEmpty()) {
      throw new EmptyResultDataAccessException(
          String.format(
              "Entities of entityType '%s' were not found for the following ids: '%s' .",
              resolveEntityTypeName(entityType), nonExistingIds),
          requestedIds.size());
    }

    return entitySet;
  }

  public static <T extends NameableEntity<ID>, ID> List<T> checkExistenceByName(
      @NonNull Class<T> entityType,
      @NonNull BaseRepository<T, ID> repository,
      @NonNull List<String> names) {

    val entities = repository.findAllByNameIn(names);
    val requestedNames = ImmutableSet.copyOf(names);
    val existingNames = mapToSet(entities, NameableEntity::getName);
    val nonExistingNames = CollectionUtils.difference(requestedNames, existingNames);

    if (!nonExistingNames.isEmpty()) {
      val msg =
          String.format(
              "The following %s names do not exist: %s",
              resolveEntityTypeName(entityType), Joiner.on(" , ").join(nonExistingNames));
      log.error(msg);
      throw new NotFoundException(msg);
    }
    return entities;
  }

  public static <T extends NameableEntity<ID>, ID> List<T> checkDuplicate(
      @NonNull Class<T> entityType,
      @NonNull BaseRepository<T, ID> repository,
      @NonNull List<String> names) {

    val entities = repository.findAllByNameIn(names);
    val requestedNames = ImmutableSet.copyOf(names);
    val existingNames = mapToSet(entities, NameableEntity::getName);
    val duplicateNames = CollectionUtils.intersection(requestedNames, existingNames);

    if (!duplicateNames.isEmpty()) {
      val msg =
          String.format(
              "The following %s names already exist: %s",
              resolveEntityTypeName(entityType), Joiner.on(" , ").join(duplicateNames));
      throw new DuplicateKeyException(msg);
    }
    return entities;
  }

  public static void checkEmpty(@NonNull List<String> names) {
    if (names.isEmpty() || names.contains(""))
      throw new DataIntegrityViolationException("Entity name cannot be empty.");
  }

  private static String resolveEntityTypeName(Class<?> entityType) {
    return entityType.getSimpleName();
  }
}
