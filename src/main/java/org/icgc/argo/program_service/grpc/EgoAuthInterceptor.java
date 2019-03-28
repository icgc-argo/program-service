package org.icgc.argo.program_service.grpc;

import com.google.common.collect.Iterables;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.icgc.argo.program_service.services.EgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

@Component
public class EgoAuthInterceptor implements ServerInterceptor {

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

  /**
   * Handling authorization
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface EgoAuth {
    String[] rolesAllowed() default {"ADMIN", "USER"};

    @Aspect
    @Slf4j
    @Component
    class EgoAuthAspect {
      @Around("@annotation(egoAuth)")
      public Object checkIdentity(ProceedingJoinPoint pjp, EgoAuth egoAuth) {
        val egoToken = EGO_TOKEN.get();
        val call = Iterables.get(List.of(pjp.getArgs()), 1, null);
        assert call instanceof StreamObserver;

        if (egoToken == null) {
          ((StreamObserver) call).onError(new StatusException(Status.fromCode(Status.Code.UNAUTHENTICATED)));
        } else {
          try {
            return pjp.proceed();
          } catch(Throwable e) {
            EgoAuthAspect.log.info(e.getMessage());
          }
        }

        EgoAuthAspect.log.info("check identity");
        return null;
      }
    }
  }
}

