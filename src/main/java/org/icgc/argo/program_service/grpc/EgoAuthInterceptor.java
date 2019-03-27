package org.icgc.argo.program_service.grpc;

import org.icgc.argo.program_service.services.EgoService;
import io.grpc.*;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

@Component
class EgoAuthInterceptor implements ServerInterceptor {

  private final EgoService egoService;

  public static final Context.Key<EgoService.EgoToken> EGO_TOKEN
          = Context.key("egoToken");

  public static final Metadata.Key<String> JWT_METADATA_KEY = Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);

  @Autowired
  public EgoAuthInterceptor(EgoService egoService) {
    this.egoService = egoService;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call,
          Metadata metadata,
          ServerCallHandler<ReqT, RespT> next) {
    // You need to implement validateIdentity
    String token = metadata.get(JWT_METADATA_KEY);
    val egoToken = egoService.verifyToken(token);
//    if (egoToken.isEmpty()) {
//      // Assume user not authenticated
//      call.close(Status.fromCode(Status.Code.UNAUTHENTICATED).withDescription("Cannot find ego user"),
//              new Metadata());
//      return new ServerCall.Listener() {};
//    }
    Context context = Context.current().withValue(EGO_TOKEN, egoToken.orElse(null));
    return Contexts.interceptCall(context, call, metadata, next);
  }
}
