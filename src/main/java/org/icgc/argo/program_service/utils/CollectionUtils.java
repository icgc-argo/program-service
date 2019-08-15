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

package org.icgc.argo.program_service.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.entity.IdentifiableEntity;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static java.util.stream.IntStream.range;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class CollectionUtils {

  public static <T, U> Set<U> mapToSet(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toSet());
  }

  public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toImmutableSet() {
    return Collector.of(
      ImmutableSet.Builder::new,
      ImmutableSet.Builder::add,
      (b1, b2) -> b1.addAll(b2.build()),
      ImmutableSet.Builder::build);
  }

  public static <T, U> Set<U> mapToImmutableSet(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toUnmodifiableSet());
  }

  public static <T, U> List<U> mapToImmutableList(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toUnmodifiableList());
  }

  public static <T, U> List<U> mapToList(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toList());
  }

  public static <T, U> List<U> mapToList(T[] array, Function<T, U> mapper) {
    return mapToList(asList(array), mapper);
  }

  public static List<UUID> convertToUUIDImmutableList(Collection<String> uuids) {
    return mapToImmutableList(uuids, UUID::fromString);
  }

  public static Set<UUID> convertToUUIDImmutableSet(Collection<String> uuids) {
    return mapToImmutableSet(uuids, UUID::fromString);
  }

  public static <ID, T extends IdentifiableEntity<ID>> Set<ID> convertToIds(Collection<T> entities) {
    return mapToImmutableSet(entities, IdentifiableEntity::getId);
  }

  public static <T> Set<T> findDuplicates(Collection<T> collection) {
    val exitingSet = Sets.<T>newHashSet();
    val duplicateSet = Sets.<T>newHashSet();
    collection.forEach(
      x -> {
        if (exitingSet.contains(x)) {
          duplicateSet.add(x);
        } else {
          exitingSet.add(x);
        }
      });
    return duplicateSet;
  }

  public static <T> Set<T> difference(Collection<T> left, Collection<T> right) {
    return Sets.difference(ImmutableSet.copyOf(left), ImmutableSet.copyOf(right));
  }

  public static <T> Set<T> intersection(Collection<T> left, Collection<T> right) {
    return Sets.intersection(ImmutableSet.copyOf(left), ImmutableSet.copyOf(right));
  }

  public static <T> List<T> repeatedCallsOf(@NonNull Supplier<T> callback, int numberOfCalls) {
    return range(0, numberOfCalls).boxed().map(x -> callback.get()).collect(toImmutableList());
  }

  public static boolean nullOrEmpty(Collection collection) {
    return collection == null || collection.isEmpty();
  }

  public static <T> String join(Collection<T> collection, String separator) {
    return collection.stream().map(o -> o.toString()).collect(Collectors.joining(separator));
  }

}
