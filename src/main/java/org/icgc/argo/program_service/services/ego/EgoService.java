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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.User;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.ego.model.entity.EgoGroup;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.icgc.argo.program_service.services.ego.model.exceptions.EgoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.transaction.Transactional;
import javax.validation.constraints.Email;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.icgc.argo.program_service.proto.UserRole.ADMIN;
import static org.icgc.argo.program_service.services.ego.GroupName.createProgramGroupName;

//TODO [rtisma]: refactor into service and client
@Slf4j
@Service
public class EgoService {
  private final EgoRESTClient egoClient;
  private final ProgramEgoGroupRepository programEgoGroupRepository;
  private final ProgramConverter programConverter;
  private RSAPublicKey egoPublicKey;

  @Autowired
  public EgoService(
    @NonNull ProgramEgoGroupRepository programEgoGroupRepository,
    @NonNull ProgramConverter programConverter,
    @NonNull EgoRESTClient restClient
  ) {
    this.programEgoGroupRepository = programEgoGroupRepository;
    this.programConverter = programConverter;
    this.egoClient = restClient;
  }

  @Autowired
  private void setEgoPublicKey() {
    this.egoPublicKey = egoClient.getPublicKey();
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

  //TODO: add transactional. If there are more programdb logic in the future and something fails, it will be able to roll back those changes.
  public void setUpProgram(@NonNull ProgramEntity program, @NonNull Collection<String> adminEmails) {
    val programPolicy = egoClient.createEgoPolicy("PROGRAM-" + program.getShortName());
    val dataPolicy = egoClient.createEgoPolicy("PROGRAMDATA-" + program.getShortName());

    Stream.of(UserRole.values())
      .filter(role -> !role.equals(UserRole.UNRECOGNIZED))
      .forEach(
        role -> {
          val group = ensureGroupExists(program, role);
          egoClient.assignPermission(group, programPolicy, getProgramMask(role));
          egoClient.assignPermission(group, dataPolicy, getDataMask(role));
          saveGroupIdForProgramAndRole(program, role, group.getId());

        }
      );

    adminEmails.forEach(email -> initAdmin(email, program));
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

  private EgoGroup ensureGroupExists(ProgramEntity program, UserRole role) {
    return egoClient.ensureGroupExists(createProgramGroupName(program.getShortName(), role).toString());
  }

  private void saveGroupIdForProgramAndRole(ProgramEntity program, UserRole role, UUID groupId) {
    val programEgoGroup = new ProgramEgoGroupEntity()
      .setProgram(program)
      .setRole(role)
      .setEgoGroupId(groupId);
    programEgoGroupRepository.save(programEgoGroup);
  }

  public void initAdmin(String adminEmail, ProgramEntity programEntity) {
    if (!joinProgram(adminEmail, programEntity, ADMIN)) {
      EgoUser egoUser;
      try {
        egoUser = egoClient.createEgoUser(adminEmail);
      } catch (EgoException e) {
        throw new IllegalStateException(format("Could not create ego user for: %s", adminEmail));
      }
      val joinedProgram = joinProgram(egoUser.getEmail(), programEntity, ADMIN);
      checkState(joinedProgram, "Ego user '%s' was created but could not join the program '%s'",
        programEntity.getShortName());
    }
  }

  public List<User> getUsersInGroup(UUID programId) {
    val userResults = new ArrayList<User>();

    programEgoGroupRepository.findAllByProgramId(programId).forEach(programEgoGroup -> {
      val groupId = programEgoGroup.getEgoGroupId();
      try {
        val egoUserStream = egoClient.getUsersByGroupId(groupId);
        egoUserStream.map(programConverter::egoUserToUser)
          .forEach(userResults::add);
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.error("Fail to retrieve users from ego group '{}': {}", groupId, e.getResponseBodyAsString());
      }
    });

    return userResults;
  }

  @Transactional
  public void cleanUpProgram(ProgramEntity programEntity) {
    programEgoGroupRepository.findAllByProgramId(programEntity.getId()).forEach(programEgoGroup -> {
      val egoGroupId = programEgoGroup.getEgoGroupId();
      try {
        egoClient.deleteGroup(egoGroupId);
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.error("Cannot remove group {} in ego: {}", egoGroupId, e.getResponseBodyAsString());
      }
    });

    //TODO: create mini ego client with selected functionality instead of copy multiple requests
    egoClient.removePolicyByName("PROGRAM-" + programEntity.getShortName());
    egoClient.removePolicyByName("PROGRAMDATA-" + programEntity.getShortName());
  }

  public Boolean joinProgram(@Email String email, ProgramEntity programEntity, UserRole role) {
    val user = egoClient.getUser(email).orElse(null);
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

    //TODO: [rtisma] need to check if the user ids are already associated with the group, to avoid conflicts
    try {
      egoClient.addUserToGroup(egoGroupId, user.getId());

      log.info("{} joined program {}", email, programEntity.getName());
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      log.error("Cannot {} joined program {}: {}", email, programEntity.getName(), e.getResponseBodyAsString());
    }
    return true;
  }

  public Boolean leaveProgram(@Email String email, UUID programId) {
    val user = egoClient.getUser(email).orElse(null);
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
        egoClient.removeUserFromGroup(group.getEgoGroupId(), userId);

        log.info("User {} left group {}", userId, group.getEgoGroupId());
      } catch (HttpClientErrorException | HttpServerErrorException e) {
        log.info("Cannot remove user {} from group {}: {}", userId, group.getRole(), e.getResponseBodyAsString());
      }
    });
    return true;
  }

}
