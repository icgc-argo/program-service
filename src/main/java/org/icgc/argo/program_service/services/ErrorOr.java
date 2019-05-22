package org.icgc.argo.program_service.services;

public class ErrorOr<T> {
  private T value;
  private Error error;
  private Exception exception;

  public ErrorOr(T value) {
    this.value=value;
    this.error=null;
  }
  public ErrorOr(Error error) {
    this.value=null;
    this.error=error;
  }

  public ErrorOr(Exception exception) {
    this.exception = exception;
  }

  public boolean hasValue() {
    return getValue() != null;
  }
  public boolean hasError() {
    return getError() != null;
  }
  public boolean hasException() { return getException() != null; }
  public T getValue() {
    return this.value;
  }

  public Error getError() {
    return this.error;
  }
  public Exception getException() { return this.exception; }

}
