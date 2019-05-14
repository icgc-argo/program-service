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

  public void setUpProgram(ProgramEntity program) {
    val groups = createGroups(program.getShortName());
    createPolicies(program.getShortName(), groups);

    groups.forEach(group ->{
      UserRole role;
      if (group.name.contains(UserRole.COLLABORATOR.toString())) {
        role = UserRole.COLLABORATOR;
      } else if (group.name.contains(UserRole.SUBMITTER.toString())) {
        role = UserRole.SUBMITTER;
      } else if (group.name.contains(UserRole.CURATOR.toString())) {
        role = UserRole.CURATOR;
      } else if (group.name.contains(UserRole.ADMIN.toString())) {
        role = UserRole.ADMIN;
      } else if (group.name.contains(UserRole.BANNED.toString())) {
        role = UserRole.BANNED;
      } else {
        log.error("Unrecognized group name: {}", group.name);
        return;
      }

      val programEgoGroup = new ProgramEgoGroupEntity(program, role, group.id);
      programEgoGroupRepository.save(programEgoGroup);
    });
  }

  @Transactional
  public void cleanUpProgram(ProgramEntity programEntity) {
    programEgoGroupRepository.findAllByProgramId(programEntity.getId()).forEach(programEgoGroup ->{
      val egoGroupId = programEgoGroup.getEgoGroupId();
      try {
        restTemplate.delete(appProperties.getEgoUrl() + String.format("/groups/%s", egoGroupId));
      } catch (RestClientException e) {
        log.error("Cannot remove group {} in ego", egoGroupId);
      }
    });

    val policy1 = getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {});
    val policy2 = getObject(String.format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-DATA-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {});

    policy1.ifPresent(policy -> {
      restTemplate.delete(String.format("%s/policies/%s", appProperties.getEgoUrl(), policy.id));
    });

    policy2.ifPresent(policy -> {
      restTemplate.delete(String.format("%s/policies/%s", appProperties.getEgoUrl(), policy.id));
    });
  }

  private List<Policy> createPolicies(String programShortName, List<Group> groups) {
    val policy1 = createEgoPolicy("PROGRAM-" + programShortName);

    val policy2 = createEgoPolicy("PROGRAM-DATA-" + programShortName);

    if (policy1.isPresent() && policy2.isPresent()) {
      groups.forEach(
              group -> {
                HttpEntity<PermissionRequest> request = null;
                val url1 = String.format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policy1.get().id, group.id);
                val url2 = String.format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policy2.get().id, group.id);
                try {
                  if (group.name.contains("COLLABORATOR")) {
                    restTemplate.postForObject(url1, new HttpEntity<>(new PermissionRequest("READ")), PermissionRequest.class);
                    restTemplate.postForObject(url2, new HttpEntity<>(new PermissionRequest("READ")), PermissionRequest.class);
                  } else if (group.name.contains("SUBMITTER")) {
                    restTemplate.postForObject(url1, new HttpEntity<>(new PermissionRequest("READ")), PermissionRequest.class);
                    restTemplate.postForObject(url2, new HttpEntity<>(new PermissionRequest("WRITE")), PermissionRequest.class);
                  } else if (group.name.contains("CURATOR")) {
                    restTemplate.postForObject(url1, new HttpEntity<>(new PermissionRequest("WRITE")), PermissionRequest.class);
                    restTemplate.postForObject(url2, new HttpEntity<>(new PermissionRequest("WRITE")), PermissionRequest.class);
                  } else if (group.name.contains("ADMIN")) {
                    // TODO: change to admin when ego implement it
                    restTemplate.postForObject(url1, new HttpEntity<>(new PermissionRequest("WRITE")), PermissionRequest.class);
                    restTemplate.postForObject(url2, new HttpEntity<>(new PermissionRequest("WRITE")), PermissionRequest.class);
                  } else if (group.name.contains("BANNED")) {
                    // TODO: change to admin when ego implement it
                    restTemplate.postForObject(url1, new HttpEntity<>(new PermissionRequest("DENY")), PermissionRequest.class);
                    restTemplate.postForObject(url2, new HttpEntity<>(new PermissionRequest("DENY")), PermissionRequest.class);
                  } else  {
                    log.error("Unrecognized group name: {}", group.name);
                    return;
                  }

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

  private Optional<Policy> createEgoPolicy(String policyName) {
    return createObject(new Policy(null, policyName), Policy.class, "/policies");
  }

  private List<Group> createGroups(String programShortName) {
    val groupNames = Stream.of(UserRole.values()).map(UserRole::name).map(s -> "PROGRAM-" + programShortName + "-" + s);

    return groupNames
            .map(this::createGroup)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
  }

  private Optional<Group> createGroup(String groupName) {
    val group = createObject(new Group(null, groupName, null, "APPROVED"), Group.class, "/groups");
    if (group.isEmpty()) {
      return getGroup(groupName);
    }
    return group;
  }

  <T> Optional<T> getObject(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
    return getObjects(url, typeReference).findFirst();
  }

  <T> Stream<T> getObjects(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
    try {
      ResponseEntity<EgoCollection<T>> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, typeReference);
      val collection = responseEntity.getBody();
      if (collection != null) {
        return collection.getResultSet().stream();
      }
      return Stream.empty();
    } catch(RestClientException e) {
      log.error("Cannot get ego object {}", typeReference.getType(), e);
      return Stream.empty();
    }
  }

  Optional<Group> getGroup(String groupName) {
    return getObject(String.format("%s/groups?query=%s", appProperties.getEgoUrl(), groupName), new ParameterizedTypeReference<EgoCollection<Group>>() {});
  }

  private <T> Optional<T> createObject(T egoObject, Class<T> egoObjectType, String path) {
    try {
      val request = new HttpEntity<>(egoObject);
      val obj = restTemplate.postForObject(appProperties.getEgoUrl() + path, request, egoObjectType);
      return Optional.ofNullable(obj);
    } catch(RestClientException e) {
      log.error("Cannot create ego object", e);
      return Optional.empty();
    }
  }


  Optional<User> getUser(@Email String email) {
    return getObject(String.format("%s/users?query=%s", appProperties.getEgoUrl(), email), new ParameterizedTypeReference<EgoCollection<User>>() {});
  }

  Boolean joinProgram(@Email String email, ProgramEntity programEntity, UserRole role) {
    val user = getUser(email).orElse(null);
    if (user == null) {
      log.error("Cannot find user with email {}", email);
      return false;
    }
    val programEgoGroup = programEgoGroupRepository.findByProgramIdAndRole(programEntity.getId(), role);
    if (programEgoGroup.isEmpty()) {
      log.error("Cannot find program ego group, have you created the groups in ego?");
      return false;
    }
    val egoGroupId = programEgoGroup.map(ProgramEgoGroupEntity::getEgoGroupId).get();
    val body = List.of(user.getId());
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

