package org.icgc.argo.program_service.model.entity;

public interface NameableEntity<ID> extends IdentifiableEntity<ID> {

  String getName();

}
