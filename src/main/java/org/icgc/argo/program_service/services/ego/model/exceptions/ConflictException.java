package org.icgc.argo.program_service.services.ego.model.exceptions;

import lombok.NonNull;

public class ConflictException extends RuntimeException {
  public ConflictException(@NonNull String message) {super(message);};
}
