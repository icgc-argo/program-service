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

package org.icgc.argo.program_service.grpc;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.grpc.Status.NOT_FOUND;
import static io.grpc.Status.UNKNOWN;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.ProgramServiceFacade;
import org.icgc.argo.program_service.services.auth.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {

  /** Dependencies */
  private final CommonConverter commonConverter;

  private final AuthorizationService authorizationService;
  private final ProgramServiceFacade serviceFacade;

  @Autowired
  public ProgramServiceImpl(
      @NonNull CommonConverter commonConverter,
      AuthorizationService authorizationService,
      ProgramServiceFacade serviceFacade) {
    this.commonConverter = commonConverter;
    this.authorizationService = authorizationService;
    this.serviceFacade = serviceFacade;
  }

  @Override
  public void createProgram(
      CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    authorizationService.requireDCCAdmin();
    val response = serviceFacade.createProgram(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void getProgram(
      GetProgramRequest request, StreamObserver<GetProgramResponse> responseObserver) {
    authorizationService.requireProgramUser(request.getShortName().getValue());
    val response = serviceFacade.getProgram(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateProgram(
      UpdateProgramRequest request, StreamObserver<UpdateProgramResponse> responseObserver) {
    authorizationService.requireDCCAdmin();
    try {
      val response = serviceFacade.updateProgram(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException | NoSuchElementException e) {
      log.error("Exception throw in updateProgram: {}", e.getMessage());
      throw status(NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public void activateProgram(
      ActivateProgramRequest request, StreamObserver<GetProgramResponse> responseObserver) {
    authorizationService.requireDCCAdmin();
    try {
      val response = serviceFacade.activateProgram(request);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException | NoSuchElementException e) {
      log.error("Exception throw in updateProgram: {}", e.getMessage());
      throw status(NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public void inviteUser(
      InviteUserRequest request, StreamObserver<InviteUserResponse> responseObserver) {
    authorizationService.requireProgramAdmin(request.getProgramShortName().getValue());

    try {
      val inviteUserResponse = serviceFacade.inviteUser(request);
      responseObserver.onNext(inviteUserResponse);
      responseObserver.onCompleted();
    } catch (Throwable throwable) {
      responseObserver.onError(status(throwable));
    }
  }

  @Override
  public void joinProgram(
      JoinProgramRequest request, StreamObserver<JoinProgramResponse> responseObserver) {
    try {
      val response =
          serviceFacade.joinProgram(
              request, (i) -> authorizationService.requireEmail(i.getUserEmail()));
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (NotFoundException e) {
      log.error("Exception throw in joinProgram: {}", e.getMessage());
      throw status(NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val listProgramsResponse =
        serviceFacade.listPrograms(p -> authorizationService.canRead(p.getShortName()));
    responseObserver.onNext(listProgramsResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listUsers(
      ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
    val programShortName = request.getProgramShortName().getValue();
    authorizationService.requireProgramAdmin(programShortName);

    val response = serviceFacade.listUsers(programShortName);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void removeUser(
      RemoveUserRequest request, StreamObserver<RemoveUserResponse> responseObserver) {
    val programName = request.getProgramShortName().getValue();
    authorizationService.requireProgramAdmin(programName);

    val response = serviceFacade.removeUser(request);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void updateUser(UpdateUserRequest request, StreamObserver<Empty> responseObserver) {
    authorizationService.requireProgramAdmin(request.getShortName().getValue());

    try {
      serviceFacade.updateUser(request);
    } catch (NotFoundException e) {
      log.error("Exception throw in updateUser: {}", e.getMessage());
      throw status(NOT_FOUND, e.getMessage());
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void removeProgram(RemoveProgramRequest request, StreamObserver<Empty> responseObserver) {
    authorizationService.requireDCCAdmin();

    try {
      serviceFacade.removeProgram(request);
    } catch (EmptyResultDataAccessException | InvalidDataAccessApiUsageException e) {
      log.error("Exception throw in removeProgram: {}", e.getMessage());
      throw status(NOT_FOUND, getExceptionMessage(e));
    }
    responseObserver.onNext(Empty.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void listCancers(Empty request, StreamObserver<ListCancersResponse> responseObserver) {
    val listCancersResponse = serviceFacade.listCancers();
    responseObserver.onNext(listCancersResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listPrimarySites(
      Empty request, StreamObserver<ListPrimarySitesResponse> responseObserver) {
    val listPrimarySitesResponse = serviceFacade.listPrimarySites();
    responseObserver.onNext(listPrimarySitesResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listCountries(Empty request, StreamObserver<ListCountriesResponse> responseObserver) {
    val listCountriesResponse = serviceFacade.listCountries();
    responseObserver.onNext(listCountriesResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listRegions(Empty request, StreamObserver<ListRegionsResponse> responseObserver) {
    val listRegionsResponse = serviceFacade.listRegions();
    responseObserver.onNext(listRegionsResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void listInstitutions(
      Empty request, StreamObserver<ListInstitutionsResponse> responseObserver) {
    val listInstitutionsResponse = serviceFacade.listInstitutions();
    responseObserver.onNext(listInstitutionsResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void addInstitutions(
      AddInstitutionsRequest request, StreamObserver<AddInstitutionsResponse> responseObserver) {
    val names =
        request.getNamesList().stream()
            .map(commonConverter::unboxStringValue)
            .collect(toImmutableList());
    try {
      val response = serviceFacade.addInstitutions(names);
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (DataIntegrityViolationException e) {
      log.error("Exception throw in addInstitutions: {}", e.getMessage());
      throw status(UNKNOWN, e.getMessage());
    }
  }

  @Override
  public void getJoinProgramInvite(
      GetJoinProgramInviteRequest request,
      StreamObserver<GetJoinProgramInviteResponse> responseObserver) {
    val invitation =
        serviceFacade.getInvitationById(UUID.fromString(request.getInviteId().getValue()));
    val response = GetJoinProgramInviteResponse.newBuilder().setInvitation(invitation).build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  private StatusRuntimeException status(Status code, String message) {
    return code.augmentDescription(message).asRuntimeException();
  }

  private StatusRuntimeException status(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException) {
      return (StatusRuntimeException) throwable;
    }

    return Status.UNKNOWN
        .withCause(throwable)
        .augmentDescription(throwable.getMessage())
        .asRuntimeException();
  }

  private String getExceptionMessage(NestedRuntimeException e) {
    return e.getMostSpecificCause().getMessage();
  }
}
