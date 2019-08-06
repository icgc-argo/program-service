package org.icgc.argo.program_service.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Profile("!auth")
@Service
public class DummyAuthorizationService implements AuthorizationService {
  public DummyAuthorizationService() {
    log.info("Started dummy authorization service");
  }
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
