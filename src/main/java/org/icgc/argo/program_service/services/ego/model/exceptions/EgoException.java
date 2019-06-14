package org.icgc.argo.program_service.services.ego.model.exceptions;

import lombok.NonNull;

public class EgoException extends RuntimeException {
  public EgoException(@NonNull String message) {super(message);}
}
