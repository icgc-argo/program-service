package org.icgc.argo.program_service.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.AllArgsConstructor;
import lombok.val;
import org.icgc.argo.program_service.utils.Joiners;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;

import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ExceptionListener<ReqT, RespT> extends ServerCall.Listener<ReqT> {
  private final ServerCall<ReqT, RespT> call;
  private final ServerCall.Listener<ReqT> listener;

  @Override
  public void onMessage(ReqT message) {
    try {
      listener.onMessage(message);
    } catch (Throwable e) {
      closeWithException(e);
    }
  }

  @Override
  public void onHalfClose() {
    try {
      listener.onHalfClose();
    } catch (Throwable e) {
      closeWithException(e);
    }
  }

  @Override
  public void onCancel() {
    try {
      listener.onCancel();
    } catch (Throwable e) {
      closeWithException(e);
    }
  }

  @Override
  public void onComplete() {
    try {
      listener.onComplete();
    } catch (Throwable e) {
      closeWithException(e);
    }
  }

  @Override
  public void onReady() {
    try {
      listener.onReady();
    } catch (Throwable e) {
      closeWithException(e);
    }
  }

  private void closeWithException(Throwable t) {
    StatusRuntimeException exception = getException(getCause(t));

    Metadata metadata = exception.getTrailers();
    if (metadata == null) {
      metadata = new Metadata();
    }
    call.close(exception.getStatus(), metadata);
  }

  StatusRuntimeException getException(Throwable t) {
    if (t instanceof StatusRuntimeException) {
      return (StatusRuntimeException) t;
    } else if (t instanceof javax.validation.ConstraintViolationException ) {
      return validationException((ConstraintViolationException) t);
    }
    return toStatus(t);
  }

  private StatusRuntimeException hibernateValidationException(org.hibernate.exception.ConstraintViolationException t) {
    val metadata= new Metadata();
    val name = t.getClass().getName();

    metadata.put(key("stacktrace"), Arrays.stream(t.getStackTrace()).
      map(s -> s + "\n").
      collect(Collectors.joining()));
    metadata.put(key("name"), name);
    val msg = t.getConstraintName() + "=>" + t.getMessage();
    return Status.INVALID_ARGUMENT.augmentDescription(msg).asRuntimeException(metadata);
  }

  StatusRuntimeException validationException(ConstraintViolationException e) {
     val metadata= new Metadata();
     val name = e.getClass().getName();

     metadata.put(key("stacktrace"), Arrays.stream(e.getStackTrace()).
       map(s -> s + "\n").
       collect(Collectors.joining()));
     metadata.put(key("name"), name);
     val msg = getConstraintMessage(e);
    return Status.INVALID_ARGUMENT.augmentDescription(msg).asRuntimeException(metadata);
  }

  String getConstraintMessage(ConstraintViolationException e) {
    return e.getConstraintViolations().stream().
      map(v -> v.getPropertyPath() + "=>" + v.getMessage()).collect(Collectors.joining(", "));
  }

  Throwable getCause(Throwable throwable) {
    if (throwable instanceof TransactionSystemException ||
      throwable instanceof javax.persistence.RollbackException ||
      throwable instanceof DataIntegrityViolationException ||
      throwable instanceof org.hibernate.exception.ConstraintViolationException
    ) {
      return getCause(throwable.getCause());
    }
    return throwable;
  }

  private StatusRuntimeException toStatus(Throwable t) {
    val metadata= new Metadata();
    val name = t.getClass().getName();

    metadata.put(key("stacktrace"), Arrays.stream(t.getStackTrace()).
      map(s -> s + "\n").
      collect(Collectors.joining()));
    metadata.put(key("name"), name);

    return Status.INTERNAL.augmentDescription(t.getMessage()).asRuntimeException(metadata);
  }

  private Metadata.Key<String> key(String s) {
    return Metadata.Key.of(s, Metadata.ASCII_STRING_MARSHALLER);
  }
}
