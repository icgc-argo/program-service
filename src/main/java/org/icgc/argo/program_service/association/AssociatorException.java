package org.icgc.argo.program_service.association;

import lombok.Getter;
import lombok.NonNull;

import static java.lang.String.format;
import static org.icgc.argo.program_service.association.AssociatorException.AssociatorErrorType.CONFLICT;
import static org.icgc.argo.program_service.association.AssociatorException.AssociatorErrorType.NOT_FOUND;

public class AssociatorException extends RuntimeException {

  public enum AssociatorErrorType {
    CONFLICT,
    NOT_FOUND;
  }

  @Getter
  private final AssociatorErrorType associatorErrorType;

  public AssociatorException(@NonNull AssociatorErrorType associatorErrorType, @NonNull String message) {
    super(message);
    this.associatorErrorType = associatorErrorType;
  }

  public AssociatorException(@NonNull AssociatorErrorType associatorErrorType, @NonNull  String message, @NonNull Throwable cause) {
    super(message, cause);
    this.associatorErrorType = associatorErrorType;
  }

  public static void checkConflict(boolean expression, @NonNull String formattedMessage, Object ... args){
    check(CONFLICT, expression, formattedMessage, args);
  }

  public static void checkNotFound(boolean expression, @NonNull String formattedMessage, Object ... args){
    check(NOT_FOUND, expression, formattedMessage, args);
  }

  private static void check(AssociatorErrorType errorType, boolean expression, @NonNull String formattedMessage, Object ... args){
    if (!expression){
      throw new AssociatorException(errorType, format(formattedMessage, args));
    }
  }

}
