/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

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
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.User;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.icgc.argo.program_service.proto.UserRole.ADMIN;
import static org.icgc.argo.program_service.services.EgoService.GroupName.createProgramGroupName;

//TODO [rtisma]: refactor into service and client
@Slf4j
@Service
public class EgoService {

  private final ProgramEgoGroupRepository programEgoGroupRepository;
  private ProgramRepository programRepository;
  private RestTemplate restTemplate;
  private final RetryTemplate lenientRetryTemplate;
  private final RetryTemplate retryTemplate;
  private final ProgramConverter programConverter;
  private final CommonConverter commonConverter;

  private RSAPublicKey egoPublicKey;
  private AppProperties appProperties;


  @Autowired
  public EgoService(
      @Qualifier("lenientRetryTemplate") @NonNull RetryTemplate lenientRetryTemplate,
      @NonNull RetryTemplate retryTemplate,
      @NonNull ProgramEgoGroupRepository programEgoGroupRepository,
      @NonNull ProgramRepository programRepository,
      @NonNull ProgramConverter programConverter,
      @NonNull AppProperties appProperties,
      @NonNull CommonConverter commonConverter) {
    this.programEgoGroupRepository = programEgoGroupRepository;
    this.appProperties = appProperties;
    this.lenientRetryTemplate = lenientRetryTemplate;
    this.programConverter = programConverter;
    this.retryTemplate = retryTemplate;
    this.programRepository = programRepository;
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
      val key = lenientRetryTemplate
          .execute(x -> restTemplate.getForEntity(appProperties.getEgoUrl() + "/oauth/token/public_key", String.class).getBody());
      log.info("Ego public key is fetched");
      egoPublicKey = (RSAPublicKey) Utils.getPublicKey(key, "RSA");
    } catch(HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot get public key of ego: {}", e.getResponseBodyAsString());
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
      return Optional.ofNullable(egoToken);
    } catch (JWTDecodeException exception){
      //Invalid token
      return Optional.empty();
    }
  }

  //TODO: add transactional. If there are more programdb logic in the future and something fails, it will be able to roll back those changes.
  public void setUpProgram(@NonNull ProgramEntity program, @NonNull Collection<String> adminEmails) {
    val programPolicy = createEgoPolicy("PROGRAM-" + program.getShortName());
    val dataPolicy = createEgoPolicy("PROGRAMDATA-" + program.getShortName());

    Stream.of(UserRole.values())
      .filter(role -> !role.equals(UserRole.UNRECOGNIZED))
      .forEach(
        role -> {
          val group = ensureGroupExists(program, role);
          assignPermission(group, programPolicy, getProgramMask(role));
          assignPermission(group, dataPolicy, getDataMask(role));
          saveGroupIdForProgramAndRole(program, role, group.id);

        }
      );

    adminEmails.forEach(email -> initAdmin(email, program));
  }

  private String getProgramMask(UserRole role) {
    switch(role) {
    case ADMIN: return "WRITE"; // return ADMIN
    case CURATOR: return "WRITE"; // check this with spec
    case SUBMITTER: return "READ";
    case COLLABORATOR: return "READ";
    case BANNED: return "DENY";
    default: throw new IllegalArgumentException("Invalid role " + role.toString());
    }
  }

  private String getDataMask(UserRole role) {
    switch(role) {
    case ADMIN: // return "ADMIN";
    case CURATOR: // return "ADMIN";
    case SUBMITTER:return "WRITE";
    case COLLABORATOR: return "READ";
    case BANNED: return "DENY";
    default: throw new IllegalArgumentException("Invalid role " + role.toString());
    }
  }

  private void assignPermission(Group group, Policy policy, String mask) {
    val url = format(appProperties.getEgoUrl() + "/policies/%s/permission/group/%s", policy.id, group.id);
    retry(() -> restTemplate.postForObject(url, new HttpEntity<>(new PermissionRequest(mask)), PermissionRequest.class));
  }

  private Group ensureGroupExists(ProgramEntity program, UserRole role) {
    return ensureGroupExists(createProgramGroupName(program.getShortName(), role).toString());
  }

  private void saveGroupIdForProgramAndRole(ProgramEntity program, UserRole role,  UUID groupId ) {
    val programEgoGroup = new ProgramEgoGroupEntity()
      .setProgram(program)
      .setRole(role)
      .setEgoGroupId(groupId);
    programEgoGroupRepository.save(programEgoGroup);
  }

  void initAdmin(String adminEmail, ProgramEntity programEntity){
    if (!joinProgram(adminEmail, programEntity, ADMIN)){
      EgoUser egoUser;
      try {
        egoUser = createEgoUser(adminEmail);
      } catch(EgoException e) {
        throw new IllegalStateException(format("Could not create ego user for: %s", adminEmail ));
      }
      val joinedProgram = joinProgram(egoUser.getEmail(), programEntity, ADMIN);
      checkState(joinedProgram, "Ego user '%s' was created but could not join the program '%s'", programEntity.getShortName());
    }
  }

  public List<User> getUserByGroup(UUID programId){
    val userResults = new ArrayList<User>();

    programEgoGroupRepository.findAllByProgramId(programId).forEach( programEgoGroup -> {
      val groupId = programEgoGroup.getEgoGroupId();
      try {
        val egoUserStream = getObjects(format("%s/groups/%s/users", appProperties.getEgoUrl(), groupId), new ParameterizedTypeReference<EgoCollection<EgoUser>>() {});
        egoUserStream.map(programConverter::egoUserToUser)
            .forEach(userResults::add);
      } catch (HttpClientErrorException | HttpServerErrorException e){
        log.error("Fail to retrieve users from ego group '{}': {}", groupId, e.getResponseBodyAsString());
      }
    });

    return userResults;
  }

  @Transactional
  public void cleanUpProgram(ProgramEntity programEntity) {
    programEgoGroupRepository.findAllByProgramId(programEntity.getId()).forEach(programEgoGroup ->{
      val egoGroupId = programEgoGroup.getEgoGroupId();
      try {
        retryRunnable(() -> restTemplate.delete(appProperties.getEgoUrl() + format("/groups/%s", egoGroupId)));
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.error("Cannot remove group {} in ego: {}", egoGroupId, e.getResponseBodyAsString());
      }
    });

    //TODO: create mini ego client with selected functionality instead of copy multiple requests
    val policy1 = getObject(format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAM-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {});
    val policy2 = getObject(format("%s/policies?name=%s", appProperties.getEgoUrl(), "PROGRAMDATA-" + programEntity.getShortName()), new ParameterizedTypeReference<EgoCollection<Policy>>() {});

    policy1.ifPresent(policy -> {
      retryRunnable(() -> restTemplate.delete(format("%s/policies/%s", appProperties.getEgoUrl(), policy.id)));
    });

    policy2.ifPresent(policy -> {
      retryRunnable(() -> restTemplate.delete(format("%s/policies/%s", appProperties.getEgoUrl(), policy.id)));
    });
  }

  private <T> T retry(Supplier<T> supplier){
    return retryTemplate.execute(r -> supplier.get());
  }

  private void retryRunnable(Runnable runnable){
    retryTemplate.execute(r -> {
      runnable.run();
      return r;
    });
  }



  EgoUser createEgoUser(String email) {
    val user = new EgoUser()
        .setEmail(email)
        .setStatus(StatusType.APPROVED.toString())
        //NOTE: for ticket PS-88 (https://github.com/icgc-argo/program-service/issues/88)
        .setFirstName("")
        .setLastName("")
        .setType(UserType.USER.toString());
    return createObject(user, EgoUser.class, "/users");
  }

  private Policy createEgoPolicy(String policyName) {
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

  private Group ensureGroupExists(String groupName) {
    val g = getGroup(groupName);
    if (g.isPresent()) {
      return g.get();
    }
    return createObject(new Group(null, groupName, null, "APPROVED"), Group.class, "/groups");
  }

  <T> Optional<T> getObject(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
    return getObjects(url, typeReference).findFirst();
  }

  <T> Stream<T> getObjects(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
    try {
      ResponseEntity<EgoCollection<T>> responseEntity = retry(() -> restTemplate.exchange(url, HttpMethod.GET, null, typeReference));
      val collection = responseEntity.getBody();
      if (collection != null) {
        return collection.getResultSet().stream();
      }
      return Stream.empty();
    } catch(HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot get ego object {}", typeReference.getType(), e);
      throw new EgoException(e.getResponseBodyAsString());
    }
  }

  Optional<Group> getGroup(String groupName) {
    return getObject(format("%s/groups?query=%s", appProperties.getEgoUrl(), groupName), new ParameterizedTypeReference<EgoCollection<Group>>() {});
  }

  private <T> T createObject(T egoObject, Class<T> egoObjectType, String path) {
    try {
      val request = new HttpEntity<>(egoObject);
      return retry(() -> restTemplate.postForObject(appProperties.getEgoUrl() + path, request, egoObjectType));
    } catch(HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot create ego object: {}", e.getResponseBodyAsString());

      if (e.getStatusCode() == HttpStatus.CONFLICT) {
        throw new ConflictException(format("Can't create %s : %s",
          egoObject, e.getResponseBodyAsString()));
      }

      throw new EgoException(e.getResponseBodyAsString());
    }
  }


  public void updateUserRole(@NonNull UUID userId, @NonNull UUID programId, @NonNull UserRole role) {
    getUserById(userId);
    val program = findProgramById(programId);
    val shortname = program.getShortName();

    val groups = getGroupsFromUser(userId);
    NotFoundException.checkNotFound(!groups.isEmpty(), format("No groups found for user id %s.", userId));

    // determine the current group
    for(val group : groups){
      if(isCorrectGroupName(group, shortname)) {
        if(!isSameRole(role, group.getName())){
          removeUserFromCurrentGroup(group, userId);
        } else {
          log.error(format("Cannot update user role to %s, new role is the same as current role.", role.toString()));
        }
      }
    }
    val programEgoGroup = getProgramEgoGroup(programId, role);
    val egoGroupId = programEgoGroup.getEgoGroupId();
    addUserToGroup(userId, egoGroupId);
  }

  boolean isCorrectGroupName(Group g, String shortname){
    return g.getName().toLowerCase().contains(shortname.toLowerCase());
  }

  ProgramEgoGroupEntity getProgramEgoGroup(UUID programId, UserRole role){
    return programEgoGroupRepository.findByProgramIdAndRole(programId, role)
            .orElseThrow(() -> {
              throw new NotFoundException(format("Cannot find ProgramEgoGroupEntity for programId %s and user role %s. ",
              programId, role.toString()));});
  }

  boolean isSameRole(@NonNull UserRole role, @NonNull String groupName) throws RuntimeException {
    UserRole currentRole = UserRole.UNRECOGNIZED;
    if (groupName.contains(UserRole.COLLABORATOR.toString())) {
      currentRole = UserRole.COLLABORATOR;
    } else if (groupName.contains(UserRole.SUBMITTER.toString())) {
      currentRole = UserRole.SUBMITTER;
    } else if (groupName.contains(UserRole.CURATOR.toString())) {
      currentRole = UserRole.CURATOR;
    } else if (groupName.contains(ADMIN.toString())) {
      currentRole = ADMIN;
    } else if (groupName.contains(UserRole.BANNED.toString())) {
      currentRole = UserRole.BANNED;
    } else {
      log.error("Unrecognized role {}.", currentRole.toString());
      throw new IllegalArgumentException("Unrecognized role!");
    }
    return currentRole.toString().equalsIgnoreCase(role.toString());
  }

  boolean removeUserFromCurrentGroup(@Nonnull Group group, @NonNull UUID userId){
      try {
        restTemplate.delete(appProperties.getEgoUrl() + format("/users/%s/groups/%s", userId, group.getId()));
        log.info("User {} is removed from ego group with id: {}, name: {}.", userId, group.getId(), group.getName());
        return true;
      } catch (RestClientException e) {
        log.info("Cannot remove user {} from ego group: {}", userId, group.getName());
        return false;
      }
  }

  void addUserToGroup(UUID userId, UUID egoGroupId){
    try {
      val body = singletonList(userId);
      restTemplate.postForObject(appProperties.getEgoUrl() + format("/groups/%s/users", egoGroupId),
              new HttpEntity<>(body), Group.class);
      log.info("User {} is added to ego group with id {}.", userId, egoGroupId);
    } catch (RestClientException e) {
      log.info("Cannot add user {} to ego group {}.", userId, egoGroupId);
    }
  }

  ProgramEntity findProgramById(UUID programId) throws NotFoundException {
    return programRepository.findById(programId)
            .orElseThrow(() -> {
              throw new NotFoundException(format("Program %s cannot be found.", commonConverter.uuidToString(programId)));
            });
  }

  Optional<EgoUser> getUser(@Email String email) {
    return getObject(format("%s/users?query=%s", appProperties.getEgoUrl(), email), new ParameterizedTypeReference<EgoCollection<EgoUser>>() {});
  }

  public EgoUser getUserById(UUID userId) {
    val egoUser = getFrom(format("%s/users/%s", appProperties.getEgoUrl(), userId),
            new ParameterizedTypeReference<EgoUser>() {})
            .orElseThrow(() -> {
              throw new NotFoundException(format("User %s cannot be found.", commonConverter.uuidToString(userId)));
            });
    return egoUser;
  }

  public boolean deleteUser(UUID userId){
    try {
      retryRunnable(() -> restTemplate.delete(
              appProperties.getEgoUrl() + format("/users/%s", userId)));
      log.info("User {} is deleted.");
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.info("Cannot remove user {}. ", userId, e.getResponseBodyAsString());
      return false;
    }
    return true;
  }

  List<Group> getGroupsFromUser(UUID userId){
    val groupStream = getObjects(format("%s/users/%s/groups", appProperties.getEgoUrl(), userId), new ParameterizedTypeReference<EgoCollection<Group>>() {});
    return groupStream.collect(toUnmodifiableList());
  }

  <T> Optional<T> getFrom(String url, ParameterizedTypeReference<T> typeReference) {
    return get(url, typeReference);
  }

  <T> Optional<T> get(String url, ParameterizedTypeReference<T> typeReference) {
    try {
      ResponseEntity<T> responseEntity = restTemplate.exchange(url, HttpMethod.GET, null, typeReference);
      val response = responseEntity.getBody();
      if (response != null) {
        return Optional.ofNullable(response);
      }
      return Optional.empty();
    } catch(RestClientException e) {
      log.error("Cannot get ego object {}, cause: {}.", typeReference.getType(), e.getCause());
      return Optional.empty();
    }
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
    //TODO: [rtisma] need to check if the user ids are already associated with the group, to avoid conflicts
    try {
      retry(() -> restTemplate.exchange(appProperties.getEgoUrl() + format("/groups/%s/users", egoGroupId), HttpMethod.POST, request, String.class));
      log.info("{} joined program {}", email, programEntity.getName());
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot {} joined program {}: {}", email, programEntity.getName(), e.getResponseBodyAsString());
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
        retryRunnable(() -> restTemplate.delete(
            appProperties.getEgoUrl() + format("/groups/%s/users/%s", group.getEgoGroupId(), userId)));
        log.info("User {} left group {}", userId, group.getEgoGroupId());
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.info("Cannot remove user {} from group {}: {}", userId, group.getRole(), e.getResponseBodyAsString());
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

  public static class EgoException extends RuntimeException {
    public EgoException(@NonNull String message) {super(message);}
  }

  public static class ConflictException extends RuntimeException {
    public ConflictException(@NonNull String message) {super(message);};
  }
}

