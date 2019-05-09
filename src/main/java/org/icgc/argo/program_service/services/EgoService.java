package org.icgc.argo.program_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class EgoService {

  private final ProgramEgoGroupRepository programEgoGroupRepository;
  private RestTemplate restTemplate;

  private RSAPublicKey egoPublicKey;
  private AppProperties appProperties;

  @Autowired
  public EgoService(ProgramEgoGroupRepository programEgoGroupRepository, AppProperties appProperties) {
    this.programEgoGroupRepository = programEgoGroupRepository;
    this.appProperties = appProperties;
  }

  @Autowired
  private void setRestTemplate() {
    // TODO: Maybe jwt authentication
    this.restTemplate = new RestTemplateBuilder()
            .basicAuthentication(appProperties.getEgoClientId(), this.appProperties.getEgoClientSecret())
            .setConnectTimeout(Duration.ofSeconds(10)).build();
  }

  @Autowired
  private void setEgoPublicKey() {
    RSAPublicKey egoPublicKey = null;
    try {
      log.info("Start fetching ego public key");
      val key = restTemplate.getForEntity(appProperties.getEgoUrl() + "/oauth/token/public_key", String.class).getBody();
      log.info("Ego public key is fetched");
      egoPublicKey = (RSAPublicKey) Utils.getPublicKey(key, "RSA");
    } catch(RestClientException e) {
      log.error("Cannot get public key of ego", e);
    }
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

  @AllArgsConstructor @NoArgsConstructor @Data
  private static class EgoGroup implements Serializable {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private UUID id;
    @JsonProperty
    private String name;
    @JsonProperty
    private String description;
    @JsonProperty
    private String status;
  }

  @AllArgsConstructor @NoArgsConstructor @Data
  private static class EgoPolicy implements Serializable {
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private UUID id;
    @JsonProperty()
    private String name;
  }

  @AllArgsConstructor @NoArgsConstructor @Data
  private static class EgoPermission implements Serializable {
    @JsonProperty
    private String mask;
  }

  public void setUpProgram(ProgramEntity program) {
    val groups = createEgoGroups(program.getShortName());
    createEgoPolicies(program.getShortName(), groups);

    groups.forEach(group ->{
      UserRole role;
      if (group.name.contains("COLLABORATOR")) {
        role = UserRole.COLLABORATOR;
      } else if (group.name.contains("SUBMITTER")) {
        role = UserRole.SUBMITTER;
      } else if (group.name.contains("ADMIN")) {
        role = UserRole.ADMIN;
      } else if (group.name.contains("BANNED")) {
        role = UserRole.BANNED;
      } else {
        log.error("Unrecognized group name: {}", group.name);
        return;
      }

      val programEgoGroup = new ProgramEgoGroupEntity(program, role, group.id);
      programEgoGroupRepository.save(programEgoGroup);
    });
  }

  private List<EgoPolicy> createEgoPolicies(String programShortName, List<EgoGroup> groups) {
    val policy1 = createEgoPolicy("PROGRAM-" + programShortName);

    val policy2 = createEgoPolicy("PROGRAM-DATA" + programShortName);

    if (policy1.isPresent() && policy2.isPresent()) {
      groups.forEach(
              group -> {
                HttpEntity<EgoPermission> request = null;
                if (group.name.contains("COLLABORATOR")) {
                  request = new HttpEntity<>(new EgoPermission("READ"));
                } else if (group.name.contains("SUBMITTER")) {
                  request = new HttpEntity<>(new EgoPermission("WRITE"));
                } else if (group.name.contains("ADMIN")) {
                  // TODO: change to admin when ego implement it
                  request = new HttpEntity<>(new EgoPermission("WRITE"));
                } else if (group.name.contains("BANNED")) {
                  // TODO: change to admin when ego implement it
                  request = new HttpEntity<>(new EgoPermission("DENY"));
                } else  {
                  log.error("Unrecognized group name: {}", group.name);
                  return;
                }

                val url1 = String.format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policy1.get().id, group.id);
                val url2 = String.format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policy2.get().id, group.id);
                try {
                  restTemplate.postForObject(url1, request, EgoPermission.class);
                  restTemplate.postForObject(url2, request, EgoPermission.class);
                } catch (RestClientException e) {
                  log.error("Cannot create permission", e);
                }
              }
      );
    }

    return Stream.of(policy1, policy2)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
  }

  private Optional<EgoPolicy> createEgoPolicy(String policyName) {
    try {
      val request = new HttpEntity<EgoPolicy>(new EgoPolicy(null, policyName));
      val egoPolicy = restTemplate.postForObject(appProperties.getEgoUrl() + "/policies", request, EgoPolicy.class);
      return Optional.ofNullable(egoPolicy);
    } catch(RestClientException e) {
      log.error(e.toString());
      return Optional.empty();
    }
  }

  private List<EgoGroup> createEgoGroups(String programShortName) {
    val groupNames = Stream.of(UserRole.values()).map(UserRole::name).map(s -> "PROGRAM-" + programShortName + "-" + s);

    return groupNames
            .map(this::createEgoGroup)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
  }

  private Optional<EgoGroup> createEgoGroup(String groupName) {
    try {
      val request = new HttpEntity<EgoGroup>(new EgoGroup(null, groupName, null, "APPROVED"));
      val egoGroup = restTemplate.postForObject(appProperties.getEgoUrl() + "/groups", request, EgoGroup.class);
      return Optional.ofNullable(egoGroup);
    } catch(RestClientException e) {
      log.error(e.toString());
      return Optional.empty();
    }
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

