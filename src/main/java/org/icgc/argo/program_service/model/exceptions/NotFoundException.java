package org.icgc.argo.program_service.model.exceptions;

import lombok.NonNull;

public class NotFoundException extends RuntimeException {

  public NotFoundException(@NonNull String message) {super(message);};

  public static void checkNotFound(boolean expression, @NonNull String message, @NonNull Object... args){
    if(!expression){
      throw new NotFoundException(String.format(message, args));
    }
  }

}
