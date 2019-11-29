package org.icgc.argo.program_service.services.ego.model.entity;

import com.auth0.jwt.interfaces.DecodedJWT;
import javax.validation.constraints.NotNull;
import org.icgc.argo.program_service.services.ego.Context;
import org.springframework.beans.BeanUtils;

// FIXME: This needs to be refactored because an Ego Token is more than a user
public class EgoToken extends Context.User {
  final DecodedJWT jwt;

  public EgoToken(@NotNull DecodedJWT jwt, @NotNull Context context) {
    this.jwt = jwt;
    BeanUtils.copyProperties(context.getUser(), this);
    this.setPermissions(context.getScope());
  }
}
