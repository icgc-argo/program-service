package org.icgc.argo.program_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.*;
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
      val key = restTemplate.getForEntity(appProperties.getEgoUrl() + "/oauth/token/public_key", String.class)
        .getBody();
      log.info("Ego public key is fetched");
      egoPublicKey = (RSAPublicKey) Utils.getPublicKey(key, "RSA");
    } catch (RestClientException e) {
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
    } catch (JWTDecodeException exception) {
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
    static class User {
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

  @AllArgsConstructor @NoArgsConstructor @Data
  static class Group {
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
  static class Policy {
    @JsonProperty()
    private UUID id;
    @JsonProperty()
    private String name;
  }

  @AllArgsConstructor @NoArgsConstructor @Data
  static class PermissionRequest {
    @JsonProperty
    private String mask;
  }

  @AllArgsConstructor @NoArgsConstructor @Data
  class Permission {
    @JsonProperty
    private String accessLevel;
    // etc
  }

  @AllArgsConstructor @NoArgsConstructor @Data
  static class User {
    @JsonProperty
    private UUID id;

    @JsonProperty
    private String email;

    @JsonProperty
    private String firstName;

    @JsonProperty
    private String lastName;
  }

  @AllArgsConstructor @NoArgsConstructor @Data
  static class EgoCollection<T> {
    @JsonProperty
    List<T> resultSet;

    @JsonProperty
    Integer limit;

    @JsonProperty
    Integer offset;
  }

  @SneakyThrows void setUpProgram(ProgramEntity program) {
    val programPolicy = createEgoPolicy(programPolicyName(program));
    val dataPolicy = createEgoPolicy(dataPolicyName(program));

    Stream.of(UserRole.values()).
      filter(val -> val != UserRole.UNRECOGNIZED).
      forEach(role -> {
        val group = createGroup(groupName(program, role));
        createGroupPermission(programPolicy.id, group.id, getProgramMask(role));
        createGroupPermission(dataPolicy.id, group.id, getDataMask(role));
        val programEgoGroup = new ProgramEgoGroupEntity().
          setProgram(program).
          setRole(role).
          setEgoGroupId(group.id);
        programEgoGroupRepository.save(programEgoGroup);
      });
  }

  void cleanUpProgram(ProgramEntity programEntity) {
    programEgoGroupRepository.findAllByProgramId(programEntity.getId()).
      forEach(programEgoGroup -> removeGroup(programEgoGroup.getEgoGroupId()));

    removePolicy("PROGRAM-" + programEntity.getShortName());
    removePolicy("PROGRAM-DATA-" + programEntity.getShortName());
  }

  private void removeGroup(UUID groupId) {
    try {
      deleteObject("/groups/" + groupId);
    } catch (RestClientException e) {
      log.error("Cannot remove group {} in ego", groupId);
      throw e;
    }
  }

  private void removePolicy(String policyName) {
    val policy = getPolicy(policyName);
    if (policy.isEmpty()) {
      throw new Error("Can't delete policy " + policyName + " -- it doesn't exist");
    }
    deleteObject("/policies/" + policy.get().id);
  }

  private PermissionRequest getProgramMask(UserRole role) {
    switch (role) {
    case ADMIN:
      // ego doesn't support mask "ADMIN" yet...
      //return new PermissionRequest("ADMIN");
    case CURATOR:
      return new PermissionRequest("WRITE");
    case SUBMITTER:
    case COLLABORATOR:
      return new PermissionRequest("READ");
    default:
      log.error("Unknown role " + role.name());
      return new PermissionRequest("DENY");
    }
  }

  private PermissionRequest getDataMask(UserRole role) {
    switch (role) {
    case ADMIN:
    case CURATOR:
    case SUBMITTER:
      return new PermissionRequest("WRITE");
    case COLLABORATOR:
      return new PermissionRequest("READ");
    default:
      log.error("Unknown role" + role.name());
      return new PermissionRequest("DENY");
    }
  }

  public Permission createGroupPermission(UUID policyId, UUID groupId, PermissionRequest mask) {
    val url = String.format("/policies/%s/permission/group/%s", policyId, groupId);
    return createObject(mask, Permission.class, url);
  }

  public Stream<Permission> getGroupPermissions(UUID groupId) {
    val url = String.format("/groups/%s/permissions", groupId);
    return getObjects(url, Permission.class);
  }

  private String groupName(ProgramEntity program, UserRole role) {
    return "PROGRAM-" + program.getShortName() + "-" + role.name();
  }
  private String programPolicyName(ProgramEntity program) {
    return "PROGRAM-" + program.getShortName();
  }

  private String dataPolicyName(ProgramEntity program) {
    return "PROGRAM-DATA" + program.getShortName();
  }

  @SneakyThrows
  private Policy createEgoPolicy(String policyName) {
    val p = getPolicy(policyName);
    if (p.isPresent()) {
      throw new Error("CONFLICT: policyName " + policyName + "already exists");
    }

    return createObject(new Policy(null, policyName), Policy.class, "/policies");
  }

  private Group createGroup(String groupName) {
    val group = getGroup(groupName);

    if (group.isPresent()) {
      throw new Error("CONFLICT: group" + groupName + "already exists");
    }

    return createObject(new Group(null, groupName, null, "APPROVED"), Group.class, "/groups");
  }

  <T> Optional<T> getObject(String url, Class<T> type) {
    return getObjects(url, type).findFirst();
  }

  <T> Stream<T> getObjects(String url, Class<T> type) {
    val typeReference = new ParameterizedTypeReference<EgoCollection<T>>() {};
    val full_url = appProperties.getEgoUrl() + url;
    try {
      ResponseEntity<EgoCollection<T>> responseEntity =
        restTemplate.exchange(full_url, HttpMethod.GET, null,
        typeReference);
      val collection = responseEntity.getBody();
      if (collection != null) {
        return collection.getResultSet().stream();
      }
      return Stream.empty();
    } catch (HttpClientErrorException e) {
      log.error("Cannot get ego object of type {}", type.getTypeName(), e);
      throw e;
    }
  }

  private <T> T createObject(Object egoObject, Class<T> egoObjectType, String path) {
    try {
      val request = new HttpEntity<>(egoObject);
      return restTemplate.postForObject(appProperties.getEgoUrl() + path, request, egoObjectType);
    } catch (HttpClientErrorException e) {
      log.error("Cannot create ego object", e);
      throw e;
    }
  }

  private void deleteObject(String url) {
    restTemplate.delete(appProperties.getEgoUrl() + url);
  }

 Optional<Group> getGroup(String groupName) {
    return getObjects("/groups?query=" + groupName, Group.class).
      filter(group -> group.getName().equals(groupName)).findFirst();
 }

 public Optional<Policy> getPolicy(String policyName) {
    val policies = getObjects("/policies?query=" + policyName, Policy.class);
    return policies.
      filter( policy -> policy.name.equals(policyName)).findFirst();
  }

  Optional<User> getUser(@Email String email) {
    return getObjects("/users?query=" + email, User.class).
      filter( user -> user.email.equals(email)).
      findFirst();
  }

  Optional<User> getGroupUser(UUID groupId, @Email String email) {
    val url = String.format("/groups/%s/users?query=%s", groupId, email);
    return getObjects(url, User.class).
      filter( user -> user.email.equals(email)).
      findFirst();
  }

  Boolean joinProgram(@Email String email, ProgramEntity programEntity, UserRole role) {
    val user = getUser(email);
    if (user.isEmpty()) {
      log.error("Cannot find user with email {}", email);
      return false;
    } else {
      val programEgoGroup = programEgoGroupRepository.findByProgramIdAndRole(programEntity.getId(), role);
      if (programEgoGroup.isEmpty()) {
        log.error("Cannot find program ego group, have you created the groups in ego?");
        return false;
      }
      val egoGroupId = programEgoGroup.map(ProgramEgoGroupEntity::getEgoGroupId).get();
      val body = List.of(user.get().getId());
      val request = new HttpEntity<>(body);
      try {
        restTemplate.exchange(appProperties.getEgoUrl() + String.format("/groups/%s/users", egoGroupId),
          HttpMethod.POST, request, String.class);
        log.info("{} joined program {}", email, programEntity.getName());
      } catch (RestClientException e) {
        log.error("Cannot {} joined program {}", email, programEntity.getName(), e);
      }
      return true;
    }
  }

  void leaveProgram(@Email String email, UUID programId) {
    val user = getUser(email);
    if (user.isEmpty()) {
      log.error("Cannot find user with email {}", email);
      throw new Error("User not found, email=" + email);
    } else {
      leaveProgram(user.get().getId(), programId);
    }
  }

  private void removeUserFromGroup(UUID userId, ProgramEgoGroupEntity group) {
    try {
      deleteObject(String.format("/groups/%s/users/%s", group.getEgoGroupId(), userId));
    } catch (RestClientException e) {
      log.info("Cannot remove user {} from group {}", userId, group.getRole());
      throw e;
    }
  }

  public void leaveProgram(UUID userId, UUID programId) {
    val groups = programEgoGroupRepository.findAllByProgramId(programId);
    groups.forEach(group -> removeUserFromGroup(userId, group));
  }
}

