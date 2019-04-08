package org.icgc.argo.program_service.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Noop Interceptor
 */
@Slf4j
@Profile("default")
@Component
public class NoopAuthInterceptor implements AuthInterceptor{

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    log.debug("Noop Auth");
    return next.startCall(call, headers);
  }
}
