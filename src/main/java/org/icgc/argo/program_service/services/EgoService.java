package org.icgc.argo.program_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.User;
import org.icgc.argo.program_service.UserRole;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.converter.CommonConverter;
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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.icgc.argo.program_service.UserRole.ADMIN;
import static org.icgc.argo.program_service.services.EgoService.GroupName.createProgramGroupName;

@Slf4j
@Service
public class EgoService {

  private final ProgramEgoGroupRepository programEgoGroupRepository;
  private RestTemplate restTemplate;
  private final RetryTemplate simpleRetryTemplate;

  private RSAPublicKey egoPublicKey;
  private AppProperties appProperties;
  private final CommonConverter commonConverter;


  @Autowired
  public EgoService(
      @NonNull RetryTemplate simpleRetryTemplate,
      @NonNull ProgramEgoGroupRepository programEgoGroupRepository,
	  @NonNull CommonConverter commonConverter,
      @NonNull AppProperties appProperties) {
    this.programEgoGroupRepository = programEgoGroupRepository;
    this.appProperties = appProperties;
    this.simpleRetryTemplate = simpleRetryTemplate;
    this.commonConverter = commonConverter;
  }

  @Autowired
  private void setRestTemplate() {
    // TODO: Maybe jwt authentication
    this.restTemplate = new RestTemplateBuilder()
            .basicAuthentication(appProperties.getEgoClientId(), this.appProperties.getEgoClientSecret())
            .setConnectTimeout(Duration.ofSeconds(15)).build();
  }

  @Autowired
  private void setEgoPublicKey() {
    RSAPublicKey egoPublicKey = null;
    try {
      log.info("Start fetching ego public key");
      val key = simpleRetryTemplate.execute(x -> restTemplate.getForEntity(appProperties.getEgoUrl() + "/oauth/token/public_key", String.class).getBody());
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

  private Optional<UUID> getEgoGroupId(UUID programId, UserRole role) {
    val programEgoGroup = programEgoGroupRepository.findByProgramIdAndRole(programId, role);
    return programEgoGroup.map(v -> v.getProgram().getId());
  }

  //TODO: add transactional. If there are more programdb logic in the future and something fails, it will be able to roll back those changes.
  public void setUpProgram(@NonNull ProgramEntity program, @NonNull Collection<String> initialAdminEmails) {
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
      } else if (group.name.contains(ADMIN.toString())) {
        role = ADMIN;
      } else if (group.name.contains(UserRole.BANNED.toString())) {
        role = UserRole.BANNED;
      } else {
        log.error("Unrecognized group name: {}", group.name);
        return;
      }

      val programEgoGroup = new ProgramEgoGroupEntity()
          .setProgram(program)
          .setRole(role)
          .setEgoGroupId(group.id);
      programEgoGroupRepository.save(programEgoGroup);
    });
    initialAdminEmails.forEach(email -> initAdmin(email, program));
  }

  void initAdmin(String email, ProgramEntity programEntity){
    if (!joinProgram(email, programEntity, ADMIN)){
      Optional<EgoUser> egoUser = Optional.empty();
      try {
        egoUser = createEgoUser(email);
        val joinedProgram = egoUser
            .map(x -> joinProgram(email, programEntity, ADMIN))
            .orElseThrow(() -> new IllegalStateException(format("Could not create ego user for: %s", email )));
        checkState(joinedProgram, "Ego user '%s' was created but could not join the program '%s'", programEntity.getShortName());
      } catch(Throwable t){
        log.error("Could not create user: {}", t.getMessage());
      }
    }
  }

  private User convertToUser(EgoUser egoUser){
      if( egoUser == null){
        return null;
      }

    return User.newBuilder()
        .setEmail(commonConverter.boxString(egoUser.email))
        .setFirstName(commonConverter.boxString(egoUser.firstName))
        .setLastName(commonConverter.boxString(egoUser.lastName))
        .setId(commonConverter.boxString(egoUser.id.toString()))
        .build();
  }

  public List<User> getUserByGroup(UUID programId){
    val userResults = new ArrayList();

    programEgoGroupRepository.findAllByProgramId(programId).forEach( programEgoGroup -> {
      val groupId = programEgoGroup.getEgoGroupId();
      try {
        val users = getObject(String.format("%s/groups/%s/users", appProperties.getEgoUrl(), groupId),
                      new ParameterizedTypeReference<EgoCollection<EgoUser>>() {});
        users.ifPresent( user -> {
          val programUser = convertToUser(user);
          userResults.add(programUser);
        });
      } catch (RestClientException e){
        log.error("Fail to retrieve users from ego group: ", groupId);
      }
    });

    return userResults;
  }

  @Transactional
  public void cleanUpProgram(ProgramEntity programEntity) {
    programEgoGroupRepository.findAllByProgramId(programEntity.getId()).forEach(programEgoGroup ->{
      val egoGroupId = programEgoGroup.getEgoGroupId();
      try {
        restTemplate.delete(appProperties.getEgoUrl() + format("/groups/%s", egoGroupId));
      } catch (RestClientException e) {
        log.error("Cannot remove group {} in ego", egoGroupId);
      }
    });

    //TODO: create mini ego client with selected functionality instead of copy multiple requests
    val policy1 = getObject(format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {});
    val policy2 = getObject(format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAMDATA-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {});

    policy1.ifPresent(policy -> {
      restTemplate.delete(format("%s/policies/%s", appProperties.getEgoUrl(), policy.id));
    });

    policy2.ifPresent(policy -> {
      restTemplate.delete(format("%s/policies/%s", appProperties.getEgoUrl(), policy.id));
    });
  }

  private List<Policy> createPolicies(String programShortName, List<Group> groups) {
    val policy1 = createEgoPolicy("PROGRAM-" + programShortName);

    val policy2 = createEgoPolicy("PROGRAMDATA-" + programShortName);

    if (policy1.isPresent() && policy2.isPresent()) {
      groups.forEach(
              group -> {
                HttpEntity<PermissionRequest> request = null;
                val url1 = format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policy1.get().id, group.id);
                val url2 = format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policy2.get().id, group.id);
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
            .collect(toList());
  }

  Optional<EgoUser> createEgoUser(String email) {
    val user = new EgoUser()
        .setEmail(email)
        .setStatus(StatusType.APPROVED.toString())
        .setType(UserType.USER.toString());
    return createObject(user, EgoUser.class, "/users");
  }

  private Optional<Policy> createEgoPolicy(String policyName) {
    return createObject(new Policy(null, policyName), Policy.class, "/policies");
  }

  @Value
  @Builder
  public static class GroupName{

    private static final String FORMAT = "%s-%s-%s";

    @NonNull private final String contextName;
    @NonNull private final String programShortName;
    @NonNull private final UserRole role;

    @Override
    public String toString() {
      return format(FORMAT, contextName, programShortName, role.name());
    }

    public static GroupName createProgramGroupName(String name, UserRole role){
      return GroupName.builder()
          .contextName("PROGRAM")
          .programShortName(name)
          .role(role)
          .build();
    }

  }


  private List<Group> createGroups(String programShortName) {
    return Stream.of(UserRole.values())
        .filter(role -> !role.equals(UserRole.UNRECOGNIZED))
        .map(r -> createProgramGroupName(programShortName, r))
        .map(GroupName::toString)
        .map(this::createGroup)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toUnmodifiableList());
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
    return getObject(format("%s/groups?query=%s", appProperties.getEgoUrl(), groupName), new ParameterizedTypeReference<EgoCollection<Group>>() {});
  }

  private <T> Optional<T> createObject(T egoObject, Class<T> egoObjectType, String path) {
    try {
      val request = new HttpEntity<>(egoObject);
      val obj = restTemplate.postForObject(appProperties.getEgoUrl() + path, request, egoObjectType);
      return Optional.ofNullable(obj);
    } catch(RestClientException e) {
      log.error("Cannot create ego object: {}", e.getMessage());
      return Optional.empty();
    }
  }


  Optional<EgoUser> getUser(@Email String email) {
    return getObject(format("%s/users?query=%s", appProperties.getEgoUrl(), email), new ParameterizedTypeReference<EgoCollection<EgoUser>>() {});
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
      restTemplate.exchange(appProperties.getEgoUrl() + format("/groups/%s/users", egoGroupId),
              HttpMethod.POST, request, String.class);
      log.info("{} joined program {}", email, programEntity.getName());
    } catch (RestClientException e) {
      log.error("Cannot {} joined program {}", email, programEntity.getName(), e);
    }
    return true;
  }

  Boolean leaveProgram(@Email String email, UUID programId) {
    val user = getUser(email).orElse(null);
    if (user == null) {
      log.error("Cannot find user with email {}", email);
      return false;
    }
    return this.leaveProgram(user.getId(), programId);
  }

  public Boolean leaveProgram(UUID userId, UUID programId) {
    val groups = programEgoGroupRepository.findAllByProgramId(programId);
    groups.forEach(group -> {
      try {
        restTemplate.delete(appProperties.getEgoUrl() + format("/groups/%s/users/%s", group.getEgoGroupId(), userId));
        log.info("User {} left group {}", userId, group.getEgoGroupId());
      } catch (RestClientException e) {
        log.info("Cannot remove user {} from group {}", userId, group.getRole());
      }
    });
    return true;
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
  public static class EgoCollection<T> {
    @JsonProperty
    List<T> resultSet;

    @JsonProperty
    Integer limit;

    @JsonProperty
    Integer offset;
  }


  @Accessors(chain = true)
  @AllArgsConstructor @NoArgsConstructor @Data
  public static class EgoUser {
    @JsonProperty
    private UUID id;

    @JsonProperty
    private String email;

    @JsonProperty
    private String firstName;

    @JsonProperty
    private String type;

    @JsonProperty
    private String status;

    @JsonProperty
    private String lastName;
  }

  public enum StatusType {
    APPROVED;
  }

  public enum UserType {
    USER,ADMIN;
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

}

