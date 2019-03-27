package org.icgc.argo.program_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Slf4j
@Service
public class EgoService {

  private final EgoProperties properties;

  @Autowired
  public EgoService(EgoProperties egoProperties) {
    this.properties = egoProperties;
  }

  public Optional<EgoToken> verifyToken(String jwtToken) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("token", jwtToken);

    val entity = new HttpEntity<String>(headers);
    String body;

    try {
      // TODO: Verify token by public key
      val resp = restTemplate.exchange(properties.getBaseUrl() + properties.getTokenVerifyPath(), HttpMethod.GET, entity, String.class);
      body = resp.getBody();
    } catch(HttpClientErrorException.BadRequest e) {
      log.info("JWT token cannot be verified");
      return Optional.empty();
    }

    if (body != null && body.equals("true")) {
      return parseToken(jwtToken);
    } else {
      return Optional.empty();
    }
  }

  private Optional<EgoToken> parseToken(String jwtToken) {
    try {
      DecodedJWT jwt = JWT.decode(jwtToken);
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
        // TODO: figure out what this field do
        public String[] permissions;
      }
    }
  }
}

@Component
@ConfigurationProperties(prefix="ego")
@Validated
class EgoProperties {
  @Setter(AccessLevel.PUBLIC) @Getter(AccessLevel.PUBLIC)
  @NotNull
  private String baseUrl;

  @Setter(AccessLevel.PUBLIC) @Getter(AccessLevel.PUBLIC)
  @NotNull
  private String tokenVerifyPath;
}
