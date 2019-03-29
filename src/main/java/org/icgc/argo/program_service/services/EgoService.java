package org.icgc.argo.program_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

@Slf4j
@Service
public class EgoService {

  private final RSAPublicKey egoPublicKey;

  @Autowired
  public EgoService(@Value("${ego.publicKeyUrl}") UrlResource publicKeyResource) {
    RSAPublicKey egoPublicKey = null;
    try {
      String key = Utils.toString(publicKeyResource.getInputStream());
      egoPublicKey = (RSAPublicKey) Utils.getPublicKey(key, "RSA");
    } catch(IOException e) {
      log.info("Cannot get public key of ego");
    }
    this.egoPublicKey = egoPublicKey;
  }

  public EgoService(RSAPublicKey egoPublicKey) {
    this.egoPublicKey = egoPublicKey;
  }

  public Optional<EgoToken> verifyToken(String jwtToken) {
    try {
      Algorithm algorithm = Algorithm.RSA256(this.egoPublicKey, null);
      JWTVerifier verifier = JWT.require(algorithm)
              .withIssuer("ego")
              .build(); //Reusable verifier instance
      val jwt = verifier.verify(jwtToken);
      return parseToken(jwt);
    } catch (JWTVerificationException e) {
      return Optional.empty();
    }
  }

  private Optional<EgoToken> parseToken(DecodedJWT jwt) {
    try {
      EgoToken egoToken = new EgoToken(jwt, jwt.getClaim("context").as(EgoToken.Context.class));
      return Optional.of(egoToken);
    } catch (JWTDecodeException exception){
      //Invalid token
      return Optional.empty();
    }
  }

  @AllArgsConstructor
  public static class EgoToken {
    public final DecodedJWT jwt;

    public final Context context;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Context {
      //      public String[] scope;
      public User user;

      @Data
      @JsonIgnoreProperties(ignoreUnknown = true)
      static class User {
        public String name;
        public String email;
        public String status;
        public String firstName;
        public String lastName;
        public String test;
        public String createdAt;
        public String lastLogin;
        public String preferredLanguage;
        public String type;
        public String[] roles;
        public String[] groups;
        public String[] permissions;
      }
    }
  }
}

