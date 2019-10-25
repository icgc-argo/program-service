package org.icgc.argo.program_service.services;

import io.grpc.Status;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.*;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static io.grpc.Status.NOT_FOUND;
import static java.lang.String.format;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;

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
  public ProgramEntity createProgram(CreateProgramRequest request) {
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
        programService.createWithSideEffectTransactional(
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
    return programEntity;
  }

  @Transactional
  public ProgramEntity getProgram(GetProgramRequest request) {
    val shortName = request.getShortName().getValue();
    return programService.getProgram(shortName);
  }

  @Transactional
  public ProgramEntity updateProgram(UpdateProgramRequest request) {
    val program = request.getProgram();
    val updatingProgram = programConverter.programToProgramEntity(program);
    return programService.updateProgram(
        updatingProgram,
        program.getCancerTypesList(),
        program.getPrimarySitesList(),
        program.getInstitutionsList(),
        program.getCountriesList(),
        program.getRegionsList());
  }

  @Transactional
  public UUID inviteUser(InviteUserRequest request) {
    val programShortName = request.getProgramShortName().getValue();
    val programResult = programService.getProgram(programShortName);

    val email = commonConverter.unboxStringValue(request.getEmail());
    val firstName = commonConverter.unboxStringValue(request.getFirstName());
    val lastName = commonConverter.unboxStringValue(request.getLastName());

    egoService.getOrCreateUser(email, firstName, lastName);

    return invitationService.inviteUser(programResult, email, firstName, lastName, request.getRole().getValue());
  }

  @Transactional
  public EgoUser joinProgram(JoinProgramRequest request, Consumer<JoinProgramInviteEntity> condition) {
    val str = request.getJoinProgramInvitationId().getValue();
    val id = commonConverter.stringToUUID(str);

    val invitation = invitationService.getInvitationById(id).orElseThrow(NOT_FOUND::asRuntimeException);
    condition.accept(invitation);

    return invitationService.acceptInvite(id);
  }

  @Transactional
  public List<ProgramEntity> listPrograms() {
    return programService.listPrograms();
  }

  @Transactional
  public Set<UserDetails> listUsers(String programShortName) {
    val users = egoService.getUsersInProgram(programShortName);
    Set<UserDetails> userDetails = mapToSet(users,
      user -> programConverter.userWithOptionalJoinProgramInviteToUserDetails(user,
        invitationService.getLatestInvitation(programShortName, user.getEmail().getValue())));
    userDetails.addAll(mapToList(invitationService.listPendingInvitations(programShortName),
      programConverter::joinProgramInviteToUserDetails));

    return userDetails;
  }

  @Transactional
  public void removeUser(RemoveUserRequest request) {
    val programName = request.getProgramShortName().getValue();
    val email = request.getUserEmail().getValue();
    invitationService.revoke(programName, email);
    egoService.leaveProgram(email, programName);
  }

  @Transactional
  public void updateUser(UpdateUserRequest request) {
    val programShortName = request.getShortName().getValue();
    val email = request.getUserEmail().getValue();
    val role = request.getRole().getValue();

    egoService.updateUserRole(email, programShortName, role);
  }

  @Transactional
  public void removeProgram(RemoveProgramRequest request) {
    val shortName = request.getProgramShortName().getValue();
    egoService.cleanUpProgram(shortName);
    programService.removeProgram(request.getProgramShortName().getValue());
  }

  public List<CancerEntity> listCancers() {
    return programService.listCancers();
  }

  public List<PrimarySiteEntity> listPrimarySites() {
    return programService.listPrimarySites();
  }

  public List<CountryEntity> listCountries() {
    return programService.listCountries();
  }

  public List<RegionEntity> listRegions() {
    return programService.listRegions();
  }

  public List<InstitutionEntity> listInstitutions() {
    return programService.listInstitutions();
  }

  @Transactional
  public List<InstitutionEntity> addInstitutions(List<String> names) {
    return programService.addInstitutions(names);
  }

  @Transactional
  public Optional<JoinProgramInviteEntity> getInvitationById(UUID id) {
    return invitationService.getInvitationById(id);
  }

}
