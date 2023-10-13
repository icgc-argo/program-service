/*
 * Copyright (c) 2021 The Ontario Institute for Cancer Research. All rights reserved
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
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.DataCenterConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.dto.DataCenterDTO;
import org.icgc.argo.program_service.model.dto.DataCenterRequestDTO;
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
  private final DataCenterConverter dataCenterConverter;
  private final CommonConverter commonConverter;
  private final ValidationService validationService;

  private static final String FULL_MEMBERSHIP_POLICY = "PROGRAMMEMBERSHIP-FULL";
  private static final String ASSOCIATE_MEMBERSHIP_POLICY = "PROGRAMMEMBERSHIP-ASSOCIATE";

  @Autowired
  public ProgramServiceFacade(
      @NonNull ProgramService programService,
      @NonNull EgoService egoService,
      @NonNull InvitationService invitationService,
      @NonNull ProgramConverter programConverter,
      @NonNull DataCenterConverter dataCenterConverter,
      @NonNull CommonConverter commonConverter,
      @NonNull ValidationService validationService) {
    this.programService = programService;
    this.egoService = egoService;
    this.invitationService = invitationService;
    this.programConverter = programConverter;
    this.dataCenterConverter = dataCenterConverter;
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
              initializeProgramInEgo(pe, admins);
            });
    log.debug("Created {}", programEntity.getShortName());
    return programConverter.programEntityToCreateProgramResponse(programEntity);
  }

  public GetProgramResponse getProgram(GetProgramRequest request) {
    val shortName = request.getShortName().getValue();
    val programEntity = programService.getProgram(shortName);
    val programDetails = programConverter.programEntityToProgramDetails(programEntity);
    return GetProgramResponse.newBuilder().setProgram(programDetails).build();
  }

  public ListProgramsResponse listProgramsByDataCenter(String shortName) {
    val programEntities = programService.listProgramsByDataCenter(shortName);
    return programConverter.programEntitiesToListProgramsResponse(programEntities);
  }

  @Transactional
  public UpdateProgramResponse updateProgram(UpdateProgramRequest request) {
    val program = request.getProgram();
    val updatingProgram = programConverter.programToProgramEntity(program);
    val programToUpdate = programService.getProgram(updatingProgram.getShortName(), false);

    updateMembershipPermission(programToUpdate, updatingProgram);

    val updatedProgram =
        programService.updateProgram(
            programToUpdate,
            updatingProgram,
            program.getCancerTypesList(),
            program.getPrimarySitesList(),
            program.getInstitutionsList(),
            program.getCountriesList());
    return programConverter.programEntityToUpdateProgramResponse(updatedProgram);
  }

  @Transactional
  public GetProgramResponse activateProgram(ActivateProgramRequest request) {
    val originalName = request.getOriginalShortName().getValue();
    val programEntity = programService.getProgram(originalName, true);

    // For updated name use provided value, or default to originalName
    val updatedName =
        request.getUpdatedShortName().isInitialized()
            ? request.getUpdatedShortName().getValue()
            : originalName;

    val admins = request.getAdminsList();

    // Activate it, update ego, then send response
    val updatedProgram = programService.activateProgram(programEntity, updatedName);

    initializeProgramInEgo(updatedProgram, admins);

    val programDetails = programConverter.programEntityToProgramDetails(updatedProgram);

    log.debug("Activated {} as {}", programEntity.getShortName(), updatedProgram.getShortName());
    return GetProgramResponse.newBuilder().setProgram(programDetails).build();
  }

  @Transactional
  public InviteUserResponse inviteUser(InviteUserRequest request) {
    val programShortName = request.getProgramShortName().getValue();
    val programResult = programService.getProgram(programShortName);

    val email = commonConverter.unboxStringValue(request.getEmail());
    val firstName = commonConverter.unboxStringValue(request.getFirstName());
    val lastName = commonConverter.unboxStringValue(request.getLastName());

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
        programService.listPrograms().stream()
            // Only show active programs
            .filter(p -> p.getActive().booleanValue())
            .filter(predicate)
            .collect(toList());
    return programConverter.programEntitiesToListProgramsResponse(programEntities);
  }

  public ListUsersResponse listUsers(String programShortName) {
    // Fetching the program first will throw an error if it is not active or doesnt exist
    // stopping the ego requests for a program that was never initialized
    programService.getProgram(programShortName);
    val users = egoService.getUsersInProgram(programShortName);
    Set<UserDetails> userDetails =
        mapToSet(users, user -> convertUserToUserDetail(user, programShortName));

    userDetails.addAll(
        mapToList(
            invitationService.listPendingInvitations(programShortName),
            this::convertPendingInviteToUserDetail));

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
        invitationService
            .getLatestInvitation(programShortName, email)
            .orElseThrow(
                () ->
                    NOT_FOUND
                        .withDescription("Can't update user who was never invited!")
                        .asRuntimeException());

    if (existingUserInvite.getStatus() == ACCEPTED) {
      egoService.updateUserRole(email, programShortName, role);
    } else {
      val firstName = existingUserInvite.getFirstName();
      val lastName = existingUserInvite.getLastName();
      val programResult = programService.getProgram(programShortName);

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

  private void initializeProgramInEgo(ProgramEntity pe, List<User> admins) {
    egoService.setUpProgram(pe.getShortName());
    egoService.setUpMembershipPermissions(pe.getShortName(), pe.getMembershipType());
    admins.forEach(
        admin -> {
          val email = commonConverter.unboxStringValue(admin.getEmail());
          val firstName = commonConverter.unboxStringValue(admin.getFirstName());
          val lastName = commonConverter.unboxStringValue(admin.getLastName());
          invitationService.inviteUser(pe, email, firstName, lastName, UserRole.ADMIN);
        });
  }

  private void updateMembershipPermission(
      @NonNull ProgramEntity programToUpdate, @NonNull ProgramEntity updatingProgram) {
    // check if membership type is updated:
    if (!programToUpdate.getMembershipType().equals(updatingProgram.getMembershipType())
        && !updatingProgram.getMembershipType().equals(MembershipType.UNRECOGNIZED)) {

      val shortName = updatingProgram.getShortName();
      val adminGroupId = egoService.getProgramEgoGroup(shortName, UserRole.ADMIN).getId();
      val submitterGroupId = egoService.getProgramEgoGroup(shortName, UserRole.SUBMITTER).getId();

      val fullPolicyId = egoService.getPolicyByName(FULL_MEMBERSHIP_POLICY).getId();
      val associatePolicyId = egoService.getPolicyByName(ASSOCIATE_MEMBERSHIP_POLICY).getId();

      if (updatingProgram.getMembershipType().equals(MembershipType.FULL)) {
        // delete associate membership permission from admin group and submitter group
        egoService.deleteGroupPermission(associatePolicyId, adminGroupId);
        egoService.deleteGroupPermission(associatePolicyId, submitterGroupId);

        // assign full membership permission to admin group and submitter group
        egoService.setUpMembershipPermissions(shortName, MembershipType.FULL);

      } else if (updatingProgram.getMembershipType().equals(MembershipType.ASSOCIATE)) {
        // delete existing full membership permission from admin group and submitter group
        egoService.deleteGroupPermission(fullPolicyId, adminGroupId);
        egoService.deleteGroupPermission(fullPolicyId, submitterGroupId);

        // assign associate membership permission to admin group and submitter group
        egoService.setUpMembershipPermissions(shortName, MembershipType.ASSOCIATE);
      }
    }
  }

  private UserDetails convertUserToUserDetail(User user, String programShortName) {
    return programConverter.userWithOptionalJoinProgramInviteToUserDetails(
        user,
        invitationService.getLatestInvitation(programShortName, user.getEmail().getValue()),
        egoService.isUserDacoApproved(user.getEmail().getValue()));
  }

  private UserDetails convertPendingInviteToUserDetail(JoinProgramInviteEntity invite) {
    return programConverter.joinProgramInviteToUserDetails(
        invite, egoService.isUserDacoApproved(invite.getUserEmail()));
  }

  public List<DataCenterDTO> listDataCenters() {
    val dataCenterEntities = programService.listDataCenters();
    return dataCenterEntities.stream()
        .map(s -> dataCenterConverter.dataCenterToDataCenterEntity(s))
        .collect(Collectors.toList());
  }

  public DataCenterDTO createDataCenter(DataCenterRequestDTO dataCenterRequestDTO) {
    val dataCenterEntity = programService.createDataCenter(dataCenterRequestDTO);
    return dataCenterConverter.dataCenterToDataCenterEntity(dataCenterEntity);
  }

  @Transactional
  public DataCenterDTO updateDataCenter(
      String dataCenterShortName, DataCenterRequestDTO dataCenterRequestDTO) {
    val updatingDataCenter = dataCenterConverter.dataCenterToDataCenterEntity(dataCenterRequestDTO);
    val dataCenterToUpdate = programService.findDataCenterByShortName(dataCenterShortName);
    val dataCenterEntity = programService.updateDataCenter(dataCenterToUpdate, updatingDataCenter);
    return dataCenterConverter.dataCenterToDataCenterEntity(dataCenterEntity);
  }
}
