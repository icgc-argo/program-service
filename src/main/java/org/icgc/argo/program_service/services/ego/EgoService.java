/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.services.ego;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.icgc.argo.program_service.proto.UserRole.ADMIN;
import static org.icgc.argo.program_service.proto.UserRole.SUBMITTER;
import static org.icgc.argo.program_service.services.ego.GroupName.createProgramGroupName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.constraints.Email;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.properties.AppProperties;
import org.icgc.argo.program_service.proto.MembershipType;
import org.icgc.argo.program_service.proto.User;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.services.ego.model.entity.*;
import org.icgc.argo.program_service.services.ego.model.exceptions.EgoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Slf4j
@Service
public class EgoService {

  @Getter private final EgoClient egoClient;
  private final ProgramConverter programConverter;
  private final JoinProgramInviteRepository invitationRepository;
  private final AppProperties appProperties;

  private static final String FULL_MEMBERSHIP_POLICY = "PROGRAMMEMBERSHIP-FULL";
  private static final String ASSOCIATE_MEMBERSHIP_POLICY = "PROGRAMMEMBERSHIP-ASSOCIATE";

  @Autowired
  public EgoService(
      @NonNull ProgramConverter programConverter,
      @NonNull EgoClient restClient,
      @NonNull JoinProgramInviteRepository invitationRepository,
      @NonNull AppProperties appProperties) {
    this.programConverter = programConverter;
    this.egoClient = restClient;
    this.invitationRepository = invitationRepository;
    this.appProperties = appProperties;
  }

  public static List<UserRole> roles() {
    return Stream.of(UserRole.values())
        .filter(role -> !role.equals(UserRole.UNRECOGNIZED))
        .collect(Collectors.toList());
  }

  public List<EgoGroupPermissionRequest> getPermissionsForProgram(@NonNull String shortName) {
    List<EgoGroupPermissionRequest> requests = new ArrayList<>();

    val programPolicy = "PROGRAM-" + shortName;
    val dataPolicy = "PROGRAMDATA-" + shortName;

    for (val role : roles()) {
      requests.add(
          new EgoGroupPermissionRequest(
              groupName(shortName, role), programPolicy, getProgramMask(role)));
      requests.add(
          new EgoGroupPermissionRequest(groupName(shortName, role), dataPolicy, getDataMask(role)));
    }

    return requests;
  }

  public List<EgoGroupPermissionRequest> addMembershipPermissions(
      @NonNull String shortName, @NonNull MembershipType membershipType) {
    List<EgoGroupPermissionRequest> requests = new ArrayList<>();

    // admin group and data submitter group must already exist in ego:
    val adminGroup = getProgramEgoGroup(shortName, ADMIN).getName();
    val submitterGroup = getProgramEgoGroup(shortName, SUBMITTER).getName();

    if (membershipType.equals(MembershipType.FULL)) {
      requests.add(new EgoGroupPermissionRequest(adminGroup, FULL_MEMBERSHIP_POLICY, "READ"));
      requests.add(new EgoGroupPermissionRequest(submitterGroup, FULL_MEMBERSHIP_POLICY, "READ"));
    } else if (membershipType.equals(MembershipType.ASSOCIATE)) {
      requests.add(new EgoGroupPermissionRequest(adminGroup, ASSOCIATE_MEMBERSHIP_POLICY, "READ"));
      requests.add(
          new EgoGroupPermissionRequest(submitterGroup, ASSOCIATE_MEMBERSHIP_POLICY, "READ"));
    } else
      throw new IllegalArgumentException(
          "Cannot create new group permission: Unrecognized Membership type.");

    return requests;
  }

  List<String> programGroupNames(String shortName) {
    return Stream.of(UserRole.values())
        .filter(role -> !role.equals(UserRole.UNRECOGNIZED))
        .map(role -> groupName(shortName, role))
        .collect(Collectors.toList());
  }

  public void setUpProgram(@NonNull String shortName) {
    val requests = getPermissionsForProgram(shortName);
    egoClient.assignGroupPermissions(requests);
  }

  // Creates file metadata access control permissions
  public void setUpMembershipPermissions(
      @NonNull String shortName, @NonNull MembershipType membershipType) {
    val requests = addMembershipPermissions(shortName, membershipType);
    egoClient.assignGroupPermissions(requests);
  }

  public EgoMassDeleteRequest getProgramCleanupRequest(@NonNull String programShortName) {
    return new EgoMassDeleteRequest(
        List.of("PROGRAM-" + programShortName, "PROGRAMDATA-" + programShortName),
        programGroupNames(programShortName));
  }

  private String groupName(String programShortName, UserRole role) {
    return createProgramGroupName(programShortName, role).toString();
  }

  public static String getProgramMask(UserRole role) {
    switch (role) {
      case ADMIN:
        return "WRITE";
      case CURATOR:
        return "WRITE";
      case SUBMITTER:
        return "READ";
      case COLLABORATOR:
        return "READ";
      case BANNED:
        return "DENY";
      case DEFAULT:
        return "DENY";
      default:
        throw new IllegalArgumentException("Invalid role " + role.toString());
    }
  }

  public static String getDataMask(UserRole role) {
    switch (role) {
      case ADMIN: // return "WRITE";
      case CURATOR: // return "WRITE";
      case SUBMITTER:
        return "WRITE";
      case COLLABORATOR:
        return "READ";
      case BANNED:
        return "DENY";
      case DEFAULT:
        return "DENY";
      default:
        throw new IllegalArgumentException("Invalid role " + role.toString());
    }
  }

  public void updateUserRole(
      @NonNull String userEmail, @NonNull String shortName, @NonNull UserRole role) {
    val user = egoClient.getUser(userEmail).orElse(null);
    if (user == null) {
      log.error("Cannot find ego user with email {}", userEmail);
      throw new EgoException(format("Cannot find ego user with email '%s' ", userEmail));
    }
    val groups = egoClient.getGroupsByUserId(user.getId()).collect(toUnmodifiableList());

    NotFoundException.checkNotFound(
        !groups.isEmpty(), format("No groups found for user id %s.", userEmail));

    groups.stream()
        .filter(g -> isCorrectGroupName(g, shortName))
        .forEach(g -> processUserWithGroup(role, g, user.getId()));

    val programEgoGroup = getProgramEgoGroup(shortName, role);
    val egoGroupId = programEgoGroup.getId();
    egoClient.addUserToGroup(egoGroupId, user.getId());
  }

  void processUserWithGroup(UserRole role, EgoGroup group, UUID userId) {
    if (!isSameRole(role, group.getName())) {
      egoClient.removeUserFromGroup(group.getId(), userId);
    } else {
      log.error("Cannot update user role to {}, new role is the same as current role.", role);
      throw new IllegalArgumentException(
          format("Cannot update user role to '%s', new role is the same as current role.", role));
    }
  }

  public boolean isCorrectGroupName(EgoGroup g, String shortname) {
    return g.getName().toLowerCase().contains(shortname.toLowerCase());
  }

  public EgoGroup getProgramEgoGroup(String programShortName, UserRole role) {
    if (UserRole.DEFAULT.equals(role)) {
    // Log a message and return null or throw an exception to skip the DEFAULT role
    log.info("Skipping DEFAULT role for program {}", programShortName);
    throw new NotFoundException(format("Ego group for DEFAULT role in program '%s' should not be fetched.", programShortName));
  }
    val name = groupName(programShortName, role);
    val g = egoClient.getGroupByName(name);
    return g.orElseThrow(
        () -> {
          throw new NotFoundException(format("Ego group '%s' not found.", name));
        });
  }

  public boolean isSameRole(@NonNull UserRole role, @NonNull String groupName)
      throws RuntimeException {
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
    } else if (groupName.contains(UserRole.DEFAULT.toString())) {
      currentRole = UserRole.DEFAULT;
    } else {
      log.error("Unrecognized role {}.", currentRole.toString());
      throw new IllegalArgumentException("Unrecognized role!");
    }
    return currentRole.toString().equalsIgnoreCase(role.toString());
  }

  public void initAdmin(String adminEmail, String shortName) {
    if (!joinProgram(adminEmail, shortName, ADMIN)) {
      EgoUser egoUser;
      try {
        egoUser = egoClient.createEgoUser(adminEmail, "", "");
      } catch (EgoException e) {
        throw new IllegalStateException(format("Could not create ego user for: %s", adminEmail));
      }
      val joinedProgram = joinProgram(egoUser.getEmail(), shortName, ADMIN);
      checkState(
          joinedProgram,
          "Ego user '%s' was created but could not join the program '%s'",
          shortName);
    }
  }

  public List<User> getUsersInProgram(String programShortName) {
    val userResults = new ArrayList<User>();
    for (val role : roles()) {
      if (UserRole.DEFAULT.equals(role)) {
        log.info("Skipping users fetch for DEFAULT role in program {}", programShortName);
        continue; // Skip the DEFAULT role
      }
      EgoGroup group;
      try {
        group = getProgramEgoGroup(programShortName, role);
      } catch (NotFoundException e) {
        log.error(
            "Cannot find {} group for program {}. Continue to fetch users for the remaining groups",
            role,
            programShortName,
            e);
        continue;
      }

      val groupId = group.getId();
      try {
        egoClient
            .getUsersByGroupId(groupId)
            .map(egoUser -> egoUser.setRole(role))
            .map(programConverter::egoUserToUser)
            .forEach(userResults::add);
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.error(
            "Fail to retrieve users from ego group '{}': {}", groupId, e.getResponseBodyAsString());
        throw new EgoException(format("Fail to retrieve users from ego group '%s' ", groupId), e);
      }
    }

    return userResults;
  }

  public void cleanUpProgram(@NonNull String programShortName) {
    egoClient.massDelete(getProgramCleanupRequest(programShortName));
    invitationRepository.deleteAllByProgramShortName(programShortName);
  }

  public void deleteGroupPermission(@NonNull UUID policyId, @NonNull UUID groupId) {
    log.info(format("Deleting ego policy %s from group %s.", policyId, groupId));
    egoClient.deleteGroupPermission(policyId, groupId);
  }

  public Boolean joinProgram(
      @Email String email, @NonNull String programShortName, @NonNull UserRole role) {
    val user = egoClient.getUser(email).orElse(null);
    if (user == null) {
      log.error("Cannot find user with email {}", email);
      throw new NotFoundException(
          format(
              "Cannot join user %s into program %s, user does not exist in ego.",
              email, programShortName));
    }
    val programEgoGroup = getProgramEgoGroup(programShortName, role);
    val egoGroupId = programEgoGroup.getId();

    val usersInGroup = egoClient.getUsersByGroupId(egoGroupId);
    if (usersInGroup.anyMatch(egoUser -> egoUser.getEmail().equalsIgnoreCase(email))) {
      log.error(
          "User {} has already joined ego group {} for program {}.",
          email,
          egoGroupId,
          programEgoGroup.getName(),
          programShortName);
      throw new EgoException(
          format(
              "User %s has already joined ego group %s (%s).",
              email, egoGroupId, programEgoGroup.getName()));
    }

    try {
      egoClient.addUserToGroup(egoGroupId, user.getId());
      log.info("{} joined program {}", email, programShortName);
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      throw new EgoException(
          format("Cannot join user %s to program %s", email, programShortName), e.getCause());
    }
    return true;
  }

  public Boolean leaveProgram(@Email String email, String shortName) {
    val user = egoClient.getUser(email).orElse(null);

    // User may never have joined program, but is still allowed to leave it.
    if (user == null) {
      log.info("Cannot find ego user with email {}", email);
      return true;
    }

    // If user has somehow been set up with multiple groups for this program, get all of them and
    // remove them
    List<EgoGroup> groups = Collections.EMPTY_LIST;
    String errors = "";

    try {
      groups = egoClient.getGroupsByUserId(user.getId()).collect(Collectors.toList());
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.error(
          "Cannot get ego groups for user '{}': {}", user.getId(), e.getResponseBodyAsString());
      errors = format("Cannot get ego groups for user '%s'", user.getId());
    } catch (EgoException ex) {
      log.error("Cannot get ego groups for user '{}': {}", user.getId(), ex.getMessage());
      errors = format("Cannot get ego groups for user '%s'", user.getId());
    }

    if (errors.length() != 0) {
      throw new EgoException(errors);
    }

    val programGroups =
        groups.stream()
            .filter(egoGroup -> egoGroup.getName().toLowerCase().contains(shortName.toLowerCase()))
            .collect(Collectors.toList());

    for (val group : programGroups) {
      try {
        egoClient.removeUserFromGroup(group.getId(), user.getId());
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.error(
            "Cannot remove user {} from group {}: {}",
            user.getId(),
            group.getName(),
            e.getResponseBodyAsString());
        errors += format("Cannot remove user '%s' from group '%s' ", user.getId(), group.getName());
      }
    }
    if (errors.length() == 0) {
      return true;
    }
    throw new EgoException(errors);
  }

  public EgoUser getOrCreateUser(
      @Email String email, @NonNull String firstName, @NonNull String lastName) {
    return egoClient
        .getUser(email)
        .orElseGet(
            () -> {
              return egoClient.createEgoUser(email, firstName, lastName);
            });
  }

  public EgoUser convertInvitationToEgoUser(@NonNull JoinProgramInviteEntity invite) {
    return programConverter.joinProgramInviteToEgoUser(invite);
  }

  public boolean isUserDacoApproved(@Email String email) {
    val dacoPolicyName = appProperties.getDacoApprovedPermission().getPolicyName();
    val dacoAccessLevels = appProperties.getDacoApprovedPermission().getAccessLevels();

    val user = egoClient.getUser(email).orElse(null);
    if (user == null) {
      return false;
    }

    // To check whether a user is DACO approved we need to check their resolved group permissions
    // They are DACO approved if there exists a permission which has
    // policy.name == app.dacoApprovedPermission.policyName
    // and they have accessLevel which exists in app.dacoApprovedPermission.accessLevels
    // DACOApproved Permission policyName and accessLevels are set in app properties
    val resolvedUserPermissions = egoClient.getUserResolvedPermissions(user.getId());
    return Arrays.stream(resolvedUserPermissions)
        .anyMatch(
            egoPermission ->
                egoPermission.getPolicy().getName().equals(dacoPolicyName)
                    && dacoAccessLevels.contains(egoPermission.getAccessLevel()));
  }

  public EgoPolicy getPolicyByName(@NonNull String name) {
    return egoClient
        .getPolicyByName(name)
        .orElseThrow(
            () -> {
              throw new NotFoundException(format("Ego policy %s is not found.", name));
            });
  }
}
