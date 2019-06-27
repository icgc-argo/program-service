package org.icgc.argo.program_service.services.ego.model.exceptions;

import lombok.NonNull;

public class EgoException extends RuntimeException {

  public EgoException(String message, Throwable cause) {
    super(message, cause);
  }

  public EgoException(@NonNull String message) {super(message);}
}
