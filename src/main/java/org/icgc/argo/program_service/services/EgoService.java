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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
  static class Permission {
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
  public static class EgoCollection<T> {
    @JsonProperty
    List<T> resultSet;

    @JsonProperty
    Integer limit;

    @JsonProperty
    Integer offset;
  }

  @SneakyThrows
  public void setUpProgram(ProgramEntity program) {
    val programPolicy = createEgoPolicy(programPolicyName(program));
    val dataPolicy = createEgoPolicy(dataPolicyName(program));

    Stream.of(UserRole.values()).
      filter(val -> val != UserRole.UNRECOGNIZED).
      forEach(role -> {
        val result = createGroup(groupName(program, role));
        if (result.hasError()) {
          throw result.getError();
        }
        val group = result.getValue();
        createGroupPermission(programPolicy.id, group.id, getProgramMask(role));
        createGroupPermission(dataPolicy.id, group.id, getDataMask(role));
        val programEgoGroup = new ProgramEgoGroupEntity(program, role, group.id);
        programEgoGroupRepository.save(programEgoGroup);
      });
  }

  @Transactional
  public void cleanUpProgram(ProgramEntity programEntity) {
    programEgoGroupRepository.findAllByProgramId(programEntity.getId()).forEach(programEgoGroup -> {
        val egoGroupId = programEgoGroup.getEgoGroupId();
        try {
          restTemplate.delete(appProperties.getEgoUrl() + String.format("/groups/%s", egoGroupId));
        } catch (RestClientException e) {
          log.error("Cannot remove group {} in ego", egoGroupId);
        }
      });
    ErrorOr<Policy> policy1 = getObject(String.format("%s/policies?name=%s",
      appProperties.getEgoUrl(),
      "PROGRAM-" + programEntity.getShortName()),
      new ParameterizedTypeReference<>() {
      });

    ErrorOr<Policy >policy2 = getObject(
        String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-DATA-" + programEntity.getShortName()),
        new ParameterizedTypeReference<>() {});

      restTemplate.delete(String.format("%s/policies/%s", appProperties.getEgoUrl(), policy1.getValue()));
      restTemplate.delete(String.format("%s/policies/%s", appProperties.getEgoUrl(), policy2.getValue()));
  }

  PermissionRequest getProgramMask(UserRole role) {
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

  PermissionRequest getDataMask(UserRole role) {
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

  private void createGroupPermission(UUID policyId, UUID groupId, PermissionRequest mask) {
    val url = String.format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policyId, groupId);
    val request = new HttpEntity<>(mask);
    try {
      restTemplate.postForObject(url, request, PermissionRequest.class);
    } catch (RestClientException e) {
      log.error("Cannot create permission", e);
    }
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
    val p = getEgoPolicy(policyName);
    if (!p.hasError()) {
      return p.getValue();
    }

    if (hasStatusCode(p, HttpStatus.NOT_FOUND)) {
      val p2 = createObject(new Policy(null, policyName), Policy.class, "/policies");
      if (!p2.hasError()) {
        return p2.getValue();
      }
      throw p2.getError();
    } else {
      throw p.getError();
    }
  }

  private boolean hasStatusCode(ErrorOr<?> result, HttpStatus code) {
    if (result.hasValue()) {
      return false;
    }
    val err=result.getException();
    if (!(err instanceof HttpClientErrorException)){
      return false;
    }
    val status = ((HttpClientErrorException) err).getStatusCode();
    return (status == code);
  }

  private ErrorOr<Group> createGroup(String groupName) {
    val group = getGroup(groupName);
    if (hasStatusCode(group, HttpStatus.NOT_FOUND)) {
      return createObject(new Group(null, groupName, null, "APPROVED"), Group.class, "/groups");
    }
    return group;
  }

  <T> ErrorOr<T> getObject(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
    try {
      val o = getObjects(url, typeReference).findFirst();
      val object = o.orElseThrow();
      return new ErrorOr<T>(object);
    } catch(Error error) {
      return new ErrorOr<T>(error);
    }
  }

  <T> Stream<T> getObjects(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
    try {
      ResponseEntity<EgoCollection<T>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, typeReference);
      val collection = responseEntity.getBody();
      if (collection != null) {
        return collection.getResultSet().stream();
      }
      return Stream.empty();
    } catch (HttpClientErrorException e) {
      log.error("Cannot get ego object {}", typeReference.getType(), e);
      return Stream.empty();
    }
  }
 ErrorOr<Group> getGroup(String groupName) {
    return getObject(
      String.format("%s/groups?query=%s",
      appProperties.getEgoUrl(), groupName),
      new ParameterizedTypeReference<>() {});
  }

  ErrorOr<Policy> getEgoPolicy(String policyName) {
    return getObject(
      String.format("%s/groups?query=%s",
        appProperties.getEgoUrl(), policyName),
      new ParameterizedTypeReference<>() {
      });
  }

  private <T> ErrorOr<T> createObject(T egoObject, Class<T> egoObjectType, String path) {
    try {
      val request = new HttpEntity<>(egoObject);
      val obj = restTemplate.postForObject(appProperties.getEgoUrl() + path, request, egoObjectType);
      return new ErrorOr<T>(obj);
    } catch (HttpClientErrorException e) {
      log.error("Cannot create ego object", e);
      return new ErrorOr<T>(e);
    }
  }
  ErrorOr<User> getUser(@Email String email) {
    return getObject(String.format("%s/users?query=%s", appProperties.getEgoUrl(), email), new ParameterizedTypeReference<EgoCollection<User>>() {});
  }

  Boolean joinProgram(@Email String email, ProgramEntity programEntity, UserRole role) {
    val user = getUser(email);
    if (user.hasError()) {
      log.error("Cannot find user with email {}, reason={}", email,user.getError());
      return false;
    }
    val programEgoGroup = programEgoGroupRepository.findByProgramIdAndRole(programEntity.getId(), role);
    if (programEgoGroup.isEmpty()) {
      log.error("Cannot find program ego group, have you created the groups in ego?");
      return false;
    }
    val egoGroupId = programEgoGroup.map(ProgramEgoGroupEntity::getEgoGroupId).get();
    val body = List.of(user.getValue().getId());
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

  public void leaveProgram(UUID userId, UUID programId) {
    val groups = programEgoGroupRepository.findAllByProgramId(programId);
    // TODO:rpcRemoveUser(userId, groupId);
  }
}

