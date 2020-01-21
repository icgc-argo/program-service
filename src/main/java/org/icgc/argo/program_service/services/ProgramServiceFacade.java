package org.icgc.argo.program_service.services;

import static io.grpc.Status.NOT_FOUND;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity.Status.ACCEPTED;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;

import io.grpc.Status;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ProgramServiceFacade {

  /** Dependencies */
  private final ProgramService programService;

  private final EgoService egoService;
  private final InvitationService invitationService;
  private final ProgramConverter programConverter;
  private final CommonConverter commonConverter;
  private final ValidationService validationService;

  @Autowired
  public ProgramServiceFacade(
      @NonNull ProgramService programService,
      @NonNull EgoService egoService,
      @NonNull InvitationService invitationService,
      @NonNull ProgramConverter programConverter,
      @NonNull CommonConverter commonConverter,
      @NonNull ValidationService validationService) {
    this.programService = programService;
    this.egoService = egoService;
    this.invitationService = invitationService;
    this.programConverter = programConverter;
    this.commonConverter = commonConverter;
    this.validationService = validationService;
  }

  @Transactional
  public CreateProgramResponse createProgram(CreateProgramRequest request) {
    val errors = validationService.validateCreateProgramRequest(request);
    if (errors.size() != 0) {
      throw Status.INVALID_ARGUMENT
          .augmentDescription(
              format("Cannot create program: Program errors are [%s]", join(errors, ",")))
          .asRuntimeException();
    }

    val program = request.getProgram();
    val admins = request.getAdminsList();

    // TODO: Refactor this, having a transactional side effect is no longer needed thanks to the
    // facade
    val programEntity =
        programService.createWithSideEffect(
            program,
            (ProgramEntity pe) -> {
              egoService.setUpProgram(pe.getShortName());
              admins.forEach(
                  admin -> {
                    val email = commonConverter.unboxStringValue(admin.getEmail());
                    val firstName = commonConverter.unboxStringValue(admin.getFirstName());
                    val lastName = commonConverter.unboxStringValue(admin.getLastName());
                    egoService.getOrCreateUser(email, firstName, lastName);
                    invitationService.inviteUser(pe, email, firstName, lastName, UserRole.ADMIN);
                  });
            });
    log.debug("Created {}", programEntity.getShortName());
    return programConverter.programEntityToCreateProgramResponse(programEntity);
  }

  public GetProgramResponse getProgram(GetProgramRequest request) {
    val shortName = request.getShortName().getValue();
    val programEntity = programService.getProgram(shortName);
    val programDetails = programConverter.ProgramEntityToProgramDetails(programEntity);
    return GetProgramResponse.newBuilder().setProgram(programDetails).build();
  }

  @Transactional
  public UpdateProgramResponse updateProgram(UpdateProgramRequest request) {
    val program = request.getProgram();
    val updatingProgram = programConverter.programToProgramEntity(program);
    val updatedProgram =
        programService.updateProgram(
            updatingProgram,
            program.getCancerTypesList(),
            program.getPrimarySitesList(),
            program.getInstitutionsList(),
            program.getCountriesList(),
            program.getRegionsList());
    return programConverter.programEntityToUpdateProgramResponse(updatedProgram);
  }

  @Transactional
  public InviteUserResponse inviteUser(InviteUserRequest request) {
    val programShortName = request.getProgramShortName().getValue();
    val programResult = programService.getProgram(programShortName);

    val email = commonConverter.unboxStringValue(request.getEmail());
    val firstName = commonConverter.unboxStringValue(request.getFirstName());
    val lastName = commonConverter.unboxStringValue(request.getLastName());

    egoService.getOrCreateUser(email, firstName, lastName);

    val inviteId =
        invitationService.inviteUser(
            programResult, email, firstName, lastName, request.getRole().getValue());
    return programConverter.inviteIdToInviteUserResponse(inviteId);
  }

  @Transactional
  public JoinProgramResponse joinProgram(
      JoinProgramRequest request, Consumer<JoinProgramInviteEntity> condition) {
    val str = request.getJoinProgramInvitationId().getValue();
    val id = commonConverter.stringToUUID(str);

    val invitation =
        invitationService.getInvitationById(id).orElseThrow(NOT_FOUND::asRuntimeException);
    condition.accept(invitation);

    val user = invitationService.acceptInvite(id);
    return programConverter.egoUserToJoinProgramResponse(user);
  }

  public ListProgramsResponse listPrograms(Predicate<ProgramEntity> predicate) {
    val programEntities =
        programService.listPrograms().stream().filter(predicate).collect(toList());
    return programConverter.programEntitiesToListProgramsResponse(programEntities);
  }

  public ListUsersResponse listUsers(String programShortName) {
    val users = egoService.getUsersInProgram(programShortName);
    Set<UserDetails> userDetails =
        mapToSet(
            users,
            user ->
                programConverter.userWithOptionalJoinProgramInviteToUserDetails(
                    user,
                    invitationService.getLatestInvitation(
                        programShortName, user.getEmail().getValue())));
    userDetails.addAll(
        mapToList(
            invitationService.listPendingInvitations(programShortName),
            programConverter::joinProgramInviteToUserDetails));

    return ListUsersResponse.newBuilder().addAllUserDetails(userDetails).build();
  }

  @Transactional
  public RemoveUserResponse removeUser(RemoveUserRequest request) {
    val programName = request.getProgramShortName().getValue();
    val email = request.getUserEmail().getValue();
    invitationService.revoke(programName, email);
    egoService.leaveProgram(email, programName);
    return programConverter.toRemoveUserResponse("User is successfully removed!");
  }

  @Transactional
  public void updateUser(UpdateUserRequest request) {
    val programShortName = request.getShortName().getValue();
    val email = request.getUserEmail().getValue();
    val role = request.getRole().getValue();

    val existingUserInvite =
        invitationService.getLatestInvitation(programShortName, email).orElse(null);

    if (existingUserInvite == null || existingUserInvite.getStatus() == ACCEPTED) {
      egoService.updateUserRole(email, programShortName, role);
    } else {
      val firstName = existingUserInvite.getFirstName();
      val lastName = existingUserInvite.getLastName();
      val programResult = programService.getProgram(programShortName);

      // re-invite with updated role
      invitationService.inviteUser(programResult, email, firstName, lastName, role);
    }
  }

  @Transactional
  public void removeProgram(RemoveProgramRequest request) {
    val shortName = request.getProgramShortName().getValue();
    egoService.cleanUpProgram(shortName);
    programService.removeProgram(request.getProgramShortName().getValue());
  }

  @Transactional
  public JoinProgramInvite getInvitationById(UUID id) {
    val joinProgramInvite =
        invitationService
            .getInvitationById(id)
            .orElseThrow(
                () ->
                    Status.NOT_FOUND
                        .withDescription("Invitation is not found")
                        .asRuntimeException());
    return programConverter.joinProgramInviteEntityToJoinProgramInvite(joinProgramInvite);
  }

  public ListCancersResponse listCancers() {
    return programConverter.cancerEntitiesToListCancersResponse(programService.listCancers());
  }

  public ListPrimarySitesResponse listPrimarySites() {
    return programConverter.primarySiteEntitiesToListPrimarySitesResponse(
        programService.listPrimarySites());
  }

  public ListCountriesResponse listCountries() {
    return programConverter.countryEntitiesToListCountriesResponse(programService.listCountries());
  }

  public ListRegionsResponse listRegions() {
    return programConverter.regionEntitiesToListRegionsResponse(programService.listRegions());
  }

  public ListInstitutionsResponse listInstitutions() {
    return programConverter.institutionEntitiesToListInstitutionsResponse(
        programService.listInstitutions());
  }

  public AddInstitutionsResponse addInstitutions(List<String> names) {
    return programConverter.institutionsToAddInstitutionsResponse(
        programService.addInstitutions(names));
  }
}
