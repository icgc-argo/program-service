package org.icgc.argo.program_service.exception;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.model.exceptions.ForbiddenException;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.model.exceptions.UnauthorizedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@Slf4j
@ControllerAdvice
public class ExceptionHandlers {

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<Object> handleForbiddenException(
          HttpServletRequest req, ForbiddenException ex) {
    val message = ex.getMessage();
    log.error(message);
    return new ResponseEntity<Object>(
        Map.of(
            "message", ex.getMessage(),
            "timestamp", new Date(),
            "path", req.getServletPath(),
            "error", FORBIDDEN.getReasonPhrase()),
        new HttpHeaders(),
            FORBIDDEN);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Object> handleUnauthorizedException(
      HttpServletRequest req, UnauthorizedException ex) {
    val message = ex.getMessage();
    log.error(message);
    return new ResponseEntity<Object>(
        Map.of(
            "message", ex.getMessage(),
            "timestamp", new Date(),
            "path", req.getServletPath(),
            "error", UNAUTHORIZED.getReasonPhrase()),
        new HttpHeaders(),
        UNAUTHORIZED);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Object> handleNotFoundException(
          HttpServletRequest req, NotFoundException ex) {
    val message = ex.getMessage();
    log.error(message);
    return new ResponseEntity<Object>(
            Map.of(
                    "message", ex.getMessage(),
                    "timestamp", new Date(),
                    "path", req.getServletPath(),
                    "error", NOT_FOUND.getReasonPhrase()),
            new HttpHeaders(),
            NOT_FOUND);
  }
}
