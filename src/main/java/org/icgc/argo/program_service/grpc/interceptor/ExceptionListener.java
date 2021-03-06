/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;

@Slf4j
@AllArgsConstructor
public class ExceptionListener<ReqT, RespT> extends ServerCall.Listener<ReqT> {
  private final ServerCall<ReqT, RespT> call;
  private final ServerCall.Listener<ReqT> listener;

  /**
   * According to
   * io.grpc.netty.shaded.io.netty.handler.codec.http2.Http2Exception$HeaderListSizeException, the
   * maximum size for the header is 8192. This appears to be the limit for the size of the entire
   * header.
   *
   * <p>We expect that the name of the exception class should be a few hundred bytes, at most. If we
   * leave up to 1,000 bytes for the name plus any other meta-data that gets thrown into the
   * headers, we have 7192 bytes available for the stack trace, so we need to truncate it if it's
   * longer than that.
   */
  private static final int MAX_STACKTRACE_SIZE = 7192;

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
    log.error("Closing with exception", t);
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
    } else if (t instanceof javax.validation.ConstraintViolationException) {
      return validationException((ConstraintViolationException) t);
    }
    return toStatus(t);
  }

  StatusRuntimeException validationException(ConstraintViolationException e) {
    val metadata = new Metadata();
    val name = e.getClass().getName();

    metadata.put(key("stacktrace"), Arrays.asList(e.getStackTrace()).toString());
    metadata.put(key("name"), name);
    val msg = getConstraintMessage(e);
    return Status.INVALID_ARGUMENT.augmentDescription(msg).asRuntimeException(metadata);
  }

  String getConstraintMessage(ConstraintViolationException e) {
    return e.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + "=>" + v.getMessage())
        .collect(Collectors.joining(", "));
  }

  Throwable getCause(Throwable throwable) {
    if (throwable instanceof TransactionSystemException
        || throwable instanceof javax.persistence.RollbackException
        || throwable instanceof DataIntegrityViolationException
        || throwable instanceof org.hibernate.exception.ConstraintViolationException) {
      return getCause(throwable.getCause());
    }
    return throwable;
  }

  private StatusRuntimeException toStatus(Throwable t) {
    val metadata = new Metadata();
    val name = t.getClass().getName();

    metadata.put(key("name"), name);
    metadata.put(key("stacktrace"), formatStackTrace(t, MAX_STACKTRACE_SIZE));

    return Status.INTERNAL.augmentDescription(t.getMessage()).asRuntimeException(metadata);
  }

  private Metadata.Key<String> key(String s) {
    return Metadata.Key.of(s, Metadata.ASCII_STRING_MARSHALLER);
  }

  private String formatStackTrace(Throwable throwable, int maxLength) {
    val s = Arrays.asList(throwable.getStackTrace()).toString();
    if (s.length() >= maxLength) {
      return s.substring(0, maxLength);
    }
    return s;
  }
}
