package org.icgc.argo.program_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.testcontainers.shaded.org.apache.http.auth.AuthScope;
import org.testcontainers.shaded.org.apache.http.auth.UsernamePasswordCredentials;
import org.testcontainers.shaded.org.apache.http.client.CredentialsProvider;
import org.testcontainers.shaded.org.apache.http.impl.client.BasicCredentialsProvider;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class EgoService {

  private final ProgramEgoGroupRepository programEgoGroupRepository;

  private RSAPublicKey egoPublicKey;
  private final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

  @Autowired
  public EgoService(ProgramEgoGroupRepository programEgoGroupRepository) {
    this.programEgoGroupRepository = programEgoGroupRepository;
  }

  @Autowired
  private void setEgoPublicKey(AppProperties appProperties) {
    RSAPublicKey egoPublicKey = null;
    try {
      val restTemplate = new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(10)).build();
      log.info("Start fetching ego public key");
      val key = restTemplate.getForEntity(appProperties.getEgoUrl() + "/oauth/token/public_key", String.class).getBody();
      log.info("Ego public key is fetched");
      egoPublicKey = (RSAPublicKey) Utils.getPublicKey(key, "RSA");
    } catch(RestClientException e) {
      log.error("Cannot get public key of ego", e);
    }
    this.egoPublicKey = egoPublicKey;
  }

  @Autowired
  private void setCredentialsProvider(AppProperties appProperties)  {
    val credentials = new UsernamePasswordCredentials(appProperties.getEgoClientId(), appProperties.getEgoClientSecret());
    this.credentialsProvider.setCredentials(AuthScope.ANY, credentials);
  }

  public Optional<EgoToken> verifyToken(String jwtToken) {
    try {
      Algorithm algorithm = Algorithm.RSA256(this.egoPublicKey, null);
      JWTVerifier verifier = JWT.require(algorithm)
              .withIssuer("ego")
              .build(); //Reusable verifier instance
      val jwt = verifier.verify(jwtToken);
      return parseToken(jwt);
    } catch (JWTVerificationException | NullPointerException e) {
      // Handle NPE defensively for null JWT.
      return Optional.empty();
    }
  }

  private Optional<EgoToken> parseToken(DecodedJWT jwt) {
    try {
      EgoToken egoToken = new EgoToken(jwt, jwt.getClaim("context").as(Context.class));
      return Optional.of(egoToken);
    } catch (JWTDecodeException exception){
      //Invalid token
      return Optional.empty();
    }
  }

  public static class EgoToken extends Context.User {
    final DecodedJWT jwt;

    EgoToken(@NotNull DecodedJWT jwt, @NotNull Context context) {
      this.jwt = jwt;
      BeanUtils.copyProperties(context.user, this);
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @Setter @Getter
  private static class Context {
    //      public String[] scope;
    User user;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Setter @Getter
    private static class User {
      String name;
      String email;
      String status;
      String firstName;
      String lastName;
      String test;
      String createdAt;
      String lastLogin;
      String preferredLanguage;
      String type;
      String[] groups;
      String[] permissions;
    }
  }

  private Optional<UUID> getEgoGroupId(UUID programId, UserRole role) {
    val programEgoGroup = programEgoGroupRepository.findByProgramIdAndRole(programId, role);
    return programEgoGroup.map(v -> v.getProgram().getId());
  }

  public void addUser(@Email String email, UUID programId, UserRole role) {
    val groupId = getEgoGroupId(programId, role);
    // TODO:rpcAddUser(userEmailAddr, groupId);
  }

  public void removeUser(UUID userId, UUID programId) {
    val groups = programEgoGroupRepository.findAllByProgramId(programId);
    // TODO:rpcRemoveUser(userId, groupId);
  }
}

