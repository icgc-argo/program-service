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

package org.icgc.argo.program_service.services.ego;

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.User;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.ego.model.entity.EgoGroup;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.icgc.argo.program_service.services.ego.model.exceptions.EgoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.validation.constraints.Email;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.icgc.argo.program_service.proto.UserRole.ADMIN;
import static org.icgc.argo.program_service.services.ego.GroupName.createProgramGroupName;

@Slf4j
@Service
public class EgoService {

  @Getter
  private final EgoClient egoClient;
  private final ProgramEgoGroupRepository programEgoGroupRepository;
  private final ProgramConverter programConverter;
  private final JoinProgramInviteRepository invitationRepository;

  @Autowired
  public EgoService(
    @NonNull ProgramEgoGroupRepository programEgoGroupRepository,
    @NonNull ProgramConverter programConverter,
    @NonNull EgoClient restClient,
    @NonNull JoinProgramInviteRepository invitationRepository) {
    this.programEgoGroupRepository = programEgoGroupRepository;
    this.programConverter = programConverter;
    this.egoClient = restClient;
    this.invitationRepository = invitationRepository;
  }

  //TODO: add transactional. If there are more programdb logic in the future and something fails, it will be able to roll back those changes.
  public void setUpProgram(@NonNull String shortName) {
    val programPolicy = egoClient.createEgoPolicy("PROGRAM-" + shortName);
    val dataPolicy = egoClient.createEgoPolicy("PROGRAMDATA-" + shortName);

    Stream.of(UserRole.values())
      .filter(role -> !role.equals(UserRole.UNRECOGNIZED))
      .forEach(
        role -> {
          val group = ensureGroupExists(shortName, role);
          egoClient.assignPermission(group, programPolicy, getProgramMask(role));
          egoClient.assignPermission(group, dataPolicy, getDataMask(role));
          saveGroupIdForProgramAndRole(shortName, role, group.getId());
        }
      );
  }

  public static String getProgramMask(UserRole role) {
    switch (role) {
    case ADMIN:
      return "WRITE"; // return ADMIN
    case CURATOR:
      return "WRITE"; // check this with spec
    case SUBMITTER:
      return "READ";
    case COLLABORATOR:
      return "READ";
    case BANNED:
      return "DENY";
    default:
      throw new IllegalArgumentException("Invalid role " + role.toString());
    }
  }

  public static String getDataMask(UserRole role) {
    switch (role) {
    case ADMIN: // return "ADMIN";
    case CURATOR: // return "ADMIN";
    case SUBMITTER:
      return "WRITE";
    case COLLABORATOR:
      return "READ";
    case BANNED:
      return "DENY";
    default:
      throw new IllegalArgumentException("Invalid role " + role.toString());
    }
  }

  private EgoGroup ensureGroupExists(String shortName, UserRole role) {
    return egoClient.ensureGroupExists(createProgramGroupName(shortName, role).toString());
  }

  public void updateUserRole(@NonNull String userEmail, @NonNull String shortName, @NonNull UserRole role) {
    val user = egoClient.getUser(userEmail).orElse(null);
    if (user == null) {
      log.error("Cannot find ego user with email {}", userEmail);
      throw new EgoException(format("Cannot find ego user with email '%s' ", userEmail));
    }
    val groups = egoClient.getGroupsByUserId(user.getId()).collect(toUnmodifiableList());

    NotFoundException.checkNotFound(!groups.isEmpty(), format("No groups found for user id %s.", userEmail));

    groups.stream()
      .filter(g -> isCorrectGroupName(g, shortName))
      .forEach(g -> processUserWithGroup(role, g, user.getId()));

    val programEgoGroup = getProgramEgoGroup(shortName, role);
    val egoGroupId = programEgoGroup.getEgoGroupId();
    egoClient.addUserToGroup(egoGroupId, user.getId());
  }

  void processUserWithGroup(UserRole role, EgoGroup group, UUID userId) {
    if (!isSameRole(role, group.getName())) {
      egoClient.removeUserFromGroup(group.getId(), userId);
    } else {
      log.error("Cannot update user role to {}, new role is the same as current role.", role);
      throw new IllegalArgumentException(format("Cannot update user role to '%s', new role is the same as current role.", role));
    }
  }

  public boolean isCorrectGroupName(EgoGroup g, String shortname) {
    return g.getName().toLowerCase().contains(shortname.toLowerCase());
  }

  public ProgramEgoGroupEntity getProgramEgoGroup(String programShortName, UserRole role) {
    return programEgoGroupRepository.findByProgramShortNameAndRole(programShortName, role)
      .orElseThrow(() -> {
        throw new NotFoundException(format("Cannot find ProgramEgoGroupEntity for program %s and user role %s. ",
          programShortName, role.toString()));
      });
  }

  public boolean isSameRole(@NonNull UserRole role, @NonNull String groupName) throws RuntimeException {
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

  private void saveGroupIdForProgramAndRole(String short_name, UserRole role, UUID groupId) {
    val programEgoGroup = new ProgramEgoGroupEntity()
      .setProgramShortName(short_name)
      .setRole(role)
      .setEgoGroupId(groupId);
    programEgoGroupRepository.save(programEgoGroup);
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
      checkState(joinedProgram, "Ego user '%s' was created but could not join the program '%s'",
        shortName);
    }
  }

  public List<User> getUsersInProgram(String programShortName) {
    val userResults = new ArrayList<User>();
    programEgoGroupRepository.findAllByProgramShortName(programShortName).forEach(programEgoGroup -> {
      val groupId = programEgoGroup.getEgoGroupId();
      val role = programEgoGroup.getRole();
      try {
        egoClient.getUsersByGroupId(groupId)
                .map(egoUser -> egoUser.setRole(role))
                .map(programConverter::egoUserToUser)
                .forEach(userResults::add);
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.error("Fail to retrieve users from ego group '{}': {}", groupId, e.getResponseBodyAsString());
        throw new EgoException(format("Fail to retrieve users from ego group '%s' ", groupId), e);
      }
    });
    return userResults;
  }

  public void cleanUpProgram(@NonNull String programShortName) {
    programEgoGroupRepository.findAllByProgramShortName(programShortName).forEach(programEgoGroup -> {
      val egoGroupId = programEgoGroup.getEgoGroupId();
      try {
        egoClient.deleteGroup(egoGroupId);
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.error("Cannot remove group {} in ego: {}", egoGroupId, e.getResponseBodyAsString());
      }
      programEgoGroupRepository.delete(programEgoGroup);
    });

    egoClient.removePolicyByName("PROGRAM-" + programShortName);
    egoClient.removePolicyByName("PROGRAMDATA-" + programShortName);

    invitationRepository.deleteAllByProgramShortName(programShortName);
  }

  public Boolean joinProgram(@Email String email, @NonNull String programShortName, @NonNull UserRole role) {
    val user = egoClient.getUser(email).orElse(null);
    if (user == null) {
      log.error("Cannot find user with email {}", email);
      throw new NotFoundException(format("Cannot join user %s into program %s, user does not exist in ego.", email, programShortName));
    }
    val programEgoGroup = programEgoGroupRepository.findByProgramShortNameAndRole(programShortName, role);
    if (programEgoGroup.isEmpty()) {
      log.error("Cannot find program ego group, have you created the groups in ego?");
      throw new NotFoundException("Cannot find program ego group, have you created the groups in ego?");
    }
    val egoGroupId = programEgoGroup.map(ProgramEgoGroupEntity::getEgoGroupId).get();

    val usersInGroup = egoClient.getUsersByGroupId(egoGroupId);
    if(usersInGroup.anyMatch(egoUser -> egoUser.getEmail().equalsIgnoreCase(email))){
      log.error("User {} has already joined ego group {} for program {}.", email, egoGroupId, programEgoGroup.get().getProgramShortName());
      throw new EgoException(format("User %s has already joined ego group %s.", email, egoGroupId));
    }

    try {
      egoClient.addUserToGroup(egoGroupId, user.getId());
      log.info("{} joined program {}", email, programShortName);
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      throw new EgoException(format("Cannot join user %s to program %s", email, programShortName), e.getCause());
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

    // If user has somehow been set up with multiple groups for this program, get all of them and remove them
    List<EgoGroup> groups=Collections.EMPTY_LIST;
    String errors = "";

    try {
      groups = egoClient.getGroupsByUserId(user.getId()).collect(Collectors.toList());
    } catch(HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot get ego groups for user '{}': {}", user.getId(), e.getResponseBodyAsString());
      errors = format("Cannot get ego groups for user '%s'",user.getId());
    } catch (EgoException ex) {
      log.error("Cannot get ego groups for user '{}': {}", user.getId(), ex.getMessage());
      errors = format("Cannot get ego groups for user '%s'",user.getId());
    }

    if (errors.length() != 0) {
      throw new EgoException(errors);
    }

    val programGroups = groups.stream()
        .filter(egoGroup -> egoGroup.getName().toLowerCase().contains(shortName.toLowerCase()))
        .collect(Collectors.toList());


    for(val group: programGroups) {
      try {
        egoClient.removeUserFromGroup(group.getId(), user.getId());
      } catch (HttpClientErrorException | HttpServerErrorException  e) {
        log.error("Cannot remove user {} from group {}: {}", user.getId(), group.getName(), e.getResponseBodyAsString());
        errors += format("Cannot remove user '%s' from group '%s' ",
          user.getId(), group.getName());
      }
    }
    if (errors.length() == 0) {
      return true;
    }
    throw new EgoException(errors);
  }

  public EgoUser getOrCreateUser(@Email String email, @NonNull String firstName, @NonNull String lastName){
    return egoClient.getUser(email)
            .orElseGet(() ->{
              return egoClient.createEgoUser(email, firstName, lastName);});
  }

  public EgoUser convertInvitationToEgoUser(@NonNull JoinProgramInviteEntity invite){
    return programConverter.joinProgramInviteToEgoUser(invite);
  }

}

