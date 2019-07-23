package org.icgc.argo.program_service.grpc.interceptor;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import lombok.val;
import org.springframework.stereotype.Service;

@Service
public class ExceptionInterceptor implements ServerInterceptor {
  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call,
      Metadata headers,
      ServerCallHandler<ReqT, RespT> next) {

    val listener = next.startCall(call, headers);
    return new ExceptionListener<>(call, listener);
  }
}
