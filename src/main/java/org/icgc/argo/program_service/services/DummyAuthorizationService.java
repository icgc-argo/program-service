package org.icgc.argo.program_service.services;

import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("default")
@Service
public class DummyAuthorizationService implements AuthorizationService {
  @Override
  public boolean isDCCAdmin() {
    return true;
  }

  @Override
  public boolean hasPermission(String permission) {
    return true;
  }

  @Override
  public boolean hasEmail(String email) {
    return true;
  }
}
