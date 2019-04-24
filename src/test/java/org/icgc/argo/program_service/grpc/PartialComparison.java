package org.icgc.argo.program_service.grpc;

import lombok.val;
import java.util.Comparator;
import java.util.List;

abstract class PartialComparison<T> implements Comparator<T> {
  /***
   * Convert an object to a list of comparable fields
   * @param object
   * @return A list of comparable fields to compare the object by.
   */
  abstract List<Comparable> contents(T object);

  @Override public int compare(T o1,T o2) {
    val c1 = contents(o1);
    val c2 = contents(o2);

    for(int i=0; i < c1.size(); i++) {
      Comparable a = c1.get(i);
      Comparable b = c2.get(i);

      if (!a.equals(b)) {
        return a.compareTo(b);
      }
    }
    return 0;
  }
}
