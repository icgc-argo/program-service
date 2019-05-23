/*
 * Copyright (c) 2018. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.icgc.argo.program_service.retry;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;

import static lombok.AccessLevel.PROTECTED;

/**
 * ClientRetryListener allows to inject client logic which will be executed before any statements of
 * {@link DefaultRetryListener}. If after a call to the ClientRetryListener {@code isRetry()} returns {@code FALSE} the
 * default retry logic will not be executed.
 */
@Slf4j
@Data
@FieldDefaults(level = PROTECTED)
@EqualsAndHashCode(callSuper = false)
public class ClientRetryListener extends RetryListenerSupport {

  boolean retry = true;

  @Override public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
      Throwable throwable) {
    log.info("Retrying after detecting error: {}",throwable.getMessage());
    super.onError(context, callback, throwable);
  }
}
