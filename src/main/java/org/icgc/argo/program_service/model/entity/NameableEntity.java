package org.icgc.argo.program_service.model.entity;

public interface NameableEntity<ID> extends Identifiable<ID> {

  String getName();

}
