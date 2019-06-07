package org.icgc.argo.program_service.services.ego.model.entity;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.BeanUtils;

import javax.validation.constraints.NotNull;

public class EgoToken extends Context.User {
  final DecodedJWT jwt;

  public EgoToken(@NotNull DecodedJWT jwt, @NotNull Context context) {
    this.jwt = jwt;
    BeanUtils.copyProperties(context.user, this);
  }
}
