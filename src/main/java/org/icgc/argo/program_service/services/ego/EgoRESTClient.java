package org.icgc.argo.program_service.services.ego;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.services.ego.model.entity.*;
import org.icgc.argo.program_service.services.ego.model.exceptions.ConflictException;
import org.icgc.argo.program_service.services.ego.model.exceptions.EgoException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.Email;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.String.format;

@Slf4j
@Service
public class EgoRESTClient implements EgoClient {
  private RestTemplate restTemplate;
  private final RetryTemplate lenientRetryTemplate;
  private final RetryTemplate retryTemplate;
  private final CommonConverter commonConverter;
  public EgoRESTClient(
    RetryTemplate lenientRetryTemplate,
    RetryTemplate retryTemplate,
    RestTemplate restTemplate,
    CommonConverter commonConverter
    ) {
    this.lenientRetryTemplate = lenientRetryTemplate;
    this.retryTemplate = retryTemplate;
    this.restTemplate = restTemplate;
    this.commonConverter = commonConverter;
  }

  @Override public RSAPublicKey getPublicKey() {
    RSAPublicKey egoPublicKey;
    try {
      log.info("Start fetching ego public key");
      String key = lenientRetryTemplate
        .execute(x -> restTemplate.getForEntity("/oauth/token/public_key", String.class).getBody());
      log.info("Ego public key is fetched");
      egoPublicKey = (RSAPublicKey) Utils.getPublicKey(key, "RSA");
      return egoPublicKey;
    } catch(HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot get public key of ego: {}", e.getResponseBodyAsString());
      throw new EgoException(e.getResponseBodyAsString());
    }
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

  @Override public void assignPermission(EgoGroup group, EgoPolicy policy, String mask) {
    val url = format("/policies/%s/permission/group/%s", policy.getId(), group.getId());
    retry(() -> restTemplate.postForObject(url, new HttpEntity<>(new EgoPermissionRequest(mask)), EgoPermissionRequest.class));
  }

  @Override public EgoUser createEgoUser(String email) {
    val user = new EgoUser()
      .setEmail(email)
      .setStatus(EgoStatusType.APPROVED.toString())
      //NOTE: for ticket PS-88 (https://github.com/icgc-argo/program-service/issues/88)
      .setFirstName("")
      .setLastName("")
      .setType(EgoUserType.USER.toString());
    return createObject(user, EgoUser.class, "/users");
  }

  @Override public EgoPolicy createEgoPolicy(String policyName) {
    return createObject(new EgoPolicy(null, policyName), EgoPolicy.class, "/policies");
  }

  @Override public EgoGroup ensureGroupExists(String groupName) {
    val g = getGroupByName(groupName);
    if (g.isPresent()) {
      return g.get();
    }
    return createObject(new EgoGroup(null, groupName, null, "APPROVED"), EgoGroup.class, "/groups");
  }

  private <T> Optional<T> getObject(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
    return getObjects(url, typeReference).findFirst();
  }

  private <T> Stream<T> getObjects(String url, ParameterizedTypeReference<EgoCollection<T>> typeReference) {
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

  @Override public Optional<EgoGroup> getGroupByName(String groupName) {
    return getObject(format("/groups?query=%s", groupName), new ParameterizedTypeReference<EgoCollection<EgoGroup>>() {});
  }

  private <T> T createObject(T egoObject, Class<T> egoObjectType, String path) {
    try {
      val request = new HttpEntity<>(egoObject);
      return retry(() -> restTemplate.postForObject(path, request, egoObjectType));
    } catch(HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot create ego object: {}", e.getResponseBodyAsString());

      if (e.getStatusCode() == HttpStatus.CONFLICT) {
        throw new ConflictException(format("Can't create %s : %s",
          egoObject, e.getResponseBodyAsString()));
      }

      throw new EgoException(e.getResponseBodyAsString());
    }
  }

  @Override public Optional<EgoUser> getUser(@Email String email) {
    return getObject(format("/users?query=%s", email), new ParameterizedTypeReference<EgoCollection<EgoUser>>() {});
  }

  @Override
  public EgoUser getUserById(UUID userId) {
    val egoUser = getObject(format("/users/%s", userId),
      new ParameterizedTypeReference<EgoCollection<EgoUser>>() {})
      .orElseThrow(() -> {
        throw new NotFoundException(format("User %s cannot be found.",
          commonConverter.uuidToString(userId)));
      });
    return egoUser;
  }

  @Override public Stream<EgoUser> getUsersByGroupId(UUID groupId) {
    return getObjects(format("/groups/%s/users", groupId), new ParameterizedTypeReference<EgoCollection<EgoUser>>() {});
  }

  @Override
  public Stream <EgoGroup> getGroupsByUserId(UUID userId){
    return getObjects(format("/users/%s/groups", userId), new ParameterizedTypeReference<EgoCollection<EgoGroup>>() {});
  }
  @Override public void deleteGroup(UUID egoGroupId) {
    retryRunnable(() -> restTemplate.delete(format("/groups/%s", egoGroupId)));
  }

  @Override public void deletePolicy(UUID policyId) {
    retryRunnable(() -> restTemplate.delete(format("/policies/%s",policyId)));
  }

  @Override public Optional<EgoPolicy> getPolicyByName(String name) {
    return getObject(format("/policies?name=%s", name),
      new ParameterizedTypeReference<EgoCollection<EgoPolicy>>() {});
  }

  @Override public void removePolicyByName(String name) {
    getPolicyByName(name).ifPresent(p -> { deletePolicy(p.getId()); });
  }

  @Override public void addUserToGroup(UUID egoGroupId, UUID egoUserId) {
    val body = List.of(egoUserId);
    val request = new HttpEntity<>(body);
    retry(() -> restTemplate.exchange(format("/groups/%s/users", egoGroupId),
      HttpMethod.POST, request, String.class));
  }

  @Override public void removeUserFromGroup(UUID egoGroupId, UUID userId) {
    retryRunnable(() -> restTemplate.delete(
      format("/groups/%s/users/%s", egoGroupId, userId)));
  }

  @Override public boolean isMember(UUID groupId, String email) {
    val user = getObject(String.format("/groups/%s/users?query=%s", groupId, email ),
      new ParameterizedTypeReference<EgoCollection<EgoUser>>() {});
    return user.isPresent();
  }

  @Override public EgoPermission[] getGroupPermissions(UUID groupId) {
    return getObjects(String.format("/groups/%s/permissions", groupId),
      new ParameterizedTypeReference<EgoCollection<EgoPermission>>() {}).toArray(EgoPermission[]::new);
  }
}
