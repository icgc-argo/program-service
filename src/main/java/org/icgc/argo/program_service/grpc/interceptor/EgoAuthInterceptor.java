package org.icgc.argo.program_service.grpc.interceptor;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.icgc.argo.program_service.services.EgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

@Component
@Profile("auth")
public class EgoAuthInterceptor implements AuthInterceptor {

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
    Context context = Context.current().withValue(EGO_TOKEN, egoToken.orElse(null));
    return Contexts.interceptCall(context, call, metadata, next);
  }

  /**
   * Handling authorization
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface EgoAuth {
    String[] typesAllowed() default {"ADMIN", "USER"};

    @Aspect
    @Component
    @Slf4j
    @Profile("auth")
    class EgoAuthAspect {

      @SneakyThrows
      @Around("@annotation(egoAuth)")
      public Object checkIdentity(ProceedingJoinPoint pjp, EgoAuth egoAuth) {
        val egoToken = EGO_TOKEN.get();
        val call = Iterables.get(List.of(pjp.getArgs()), 1, null);
        assert call instanceof StreamObserver;

        if (egoToken == null) {
          ((StreamObserver) call).onError(new StatusException(Status.fromCode(Status.Code.UNAUTHENTICATED)));
          return null;
        }

        val availableRoles = Sets.intersection(Set.of(egoAuth.typesAllowed()), Set.of(egoToken.getType()));

        if (availableRoles.isEmpty()) {
          ((StreamObserver) call).onError(new StatusException(Status.fromCode(Status.Code.PERMISSION_DENIED)));
          return null;
        } else {
          try {
            return pjp.proceed();
          } catch(Throwable e) {
            log.info(e.getMessage());
            throw e;
          }
        }
      }
    }
  }
}

