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

package org.icgc.argo.program_service.converter;

import static java.util.stream.Collectors.toList;

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.entity.*;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(
    config = ConverterConfig.class,
    uses = {CommonConverter.class})
public interface ProgramConverter {
  ProgramConverter INSTANCE = new ProgramConverterImpl(CommonConverter.INSTANCE);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "programCancers", ignore = true)
  @Mapping(target = "programPrimarySites", ignore = true)
  @Mapping(target = "programInstitutions", ignore = true)
  @Mapping(target = "programCountries", ignore = true)
  @Mapping(target = "programRegions", ignore = true)
  @Mapping(target = "active", constant = "true")
  @Mapping(target = "legacyShortName", ignore = true)
  ProgramEntity programToProgramEntity(Program p);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "shortName", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "programCancers", ignore = true)
  @Mapping(target = "programPrimarySites", ignore = true)
  @Mapping(target = "programInstitutions", ignore = true)
  @Mapping(target = "programCountries", ignore = true)
  @Mapping(target = "programRegions", ignore = true)
  void updateProgram(ProgramEntity updatingProgram, @MappingTarget ProgramEntity programToUpdate);

  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeShortName", ignore = true)
  @Mapping(target = "mergeDescription", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "mergeMembershipType", ignore = true)
  @Mapping(target = "mergeCommitmentDonors", ignore = true)
  @Mapping(target = "mergeSubmittedDonors", ignore = true)
  @Mapping(target = "mergeGenomicDonors", ignore = true)
  @Mapping(target = "mergeWebsite", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "cancerTypesList", ignore = true)
  @Mapping(target = "primarySitesList", ignore = true)
  @Mapping(target = "institutionsList", ignore = true)
  @Mapping(target = "countriesList", ignore = true)
  @Mapping(target = "regionsList", ignore = true)
  @Mapping(target = "mergeDataCenterId", ignore = true)
  Program programEntityToProgram(ProgramEntity entity);

  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  Cancer cancerEntityToCancer(CancerEntity entity);

  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  PrimarySite primarySiteEntityToPrimarySite(PrimarySiteEntity entity);

  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  Country countryEntityToCountry(CountryEntity entity);

  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  Region regionEntityToRegion(RegionEntity entity);

  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  Institution institutionEntityToInstitution(InstitutionEntity entity);

  @AfterMapping
  default void updateProgramFromEntity(
      @NonNull ProgramEntity entity, @NonNull @MappingTarget Program.Builder programBuilder) {
    programBuilder
        .addAllCancerTypes(entity.listCancerTypes())
        .addAllPrimarySites(entity.listPrimarySites())
        .addAllInstitutions(entity.listInstitutions())
        .addAllCountries(entity.listCountries())
        .addAllRegions(entity.listRegions());
  }

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "mergeCreatedAt", ignore = true)
  CreateProgramResponse programEntityToCreateProgramResponse(ProgramEntity p);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "mergeUpdatedAt", ignore = true)
  UpdateProgramResponse programEntityToUpdateProgramResponse(ProgramEntity p);

  default ProgramDetails programEntityToProgramDetails(ProgramEntity value) {
    val program = programEntityToProgram(value);
    val metadata = programEntityToMetadata(value);
    val legacyDetails = programEntityToLegacyData(value);
    return ProgramDetails.newBuilder()
        .setProgram(program)
        .setMetadata(metadata)
        .mergeLegacy(legacyDetails)
        .build();
  }

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "removePrograms", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "programsOrBuilderList", ignore = true)
  @Mapping(target = "programsBuilderList", ignore = true)
  // TODO: [rtisma] this is a hack for a bug in mapstruct when mapping an iterable to a wrapper
  // (non-iterable)
  //  https://github.com/mapstruct/mapstruct/issues/607#issuecomment-309547739
  @Mapping(target = "programsList", source = "programEntities")
  ListProgramsResponse programEntitiesToListProgramsResponse(
      Integer dummy, Collection<ProgramEntity> programEntities);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeMessage", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  RemoveUserResponse toRemoveUserResponse(String message);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeCreatedAt", ignore = true)
  @Mapping(target = "mergeUpdatedAt", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  Metadata programEntityToMetadata(ProgramEntity programEntity);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeLegacyShortName", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  LegacyDetails programEntityToLegacyData(ProgramEntity programEntity);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeEmail", ignore = true)
  @Mapping(target = "mergeFirstName", ignore = true)
  @Mapping(target = "mergeLastName", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "mergeRole", ignore = true)
  User egoUserToUser(EgoUser egoUser);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeUser", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "user", source = "egoUser")
  JoinProgramResponse egoUserToJoinProgramResponse(Integer dummy, EgoUser egoUser);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "type", ignore = true)
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "email", source = "userEmail")
  EgoUser joinProgramInviteToEgoUser(JoinProgramInviteEntity invite);

  default JoinProgramResponse egoUserToJoinProgramResponse(EgoUser egoUser) {
    return egoUserToJoinProgramResponse(0, egoUser);
  }

  default UserRoleValue boxUserValue(@NonNull UserRole role) {
    return UserRoleValue.newBuilder().setValue(role).build();
  }

  default ListProgramsResponse programEntitiesToListProgramsResponse(
      Collection<ProgramEntity> programEntities) {
    return programEntitiesToListProgramsResponse(0, programEntities);
  }

  default ListCancersResponse cancerEntitiesToListCancersResponse(
      Collection<CancerEntity> cancerEntities) {
    return ListCancersResponse.newBuilder()
        .addAllCancers(cancerEntities.stream().map(this::cancerEntityToCancer).collect(toList()))
        .build();
  }

  default ListPrimarySitesResponse primarySiteEntitiesToListPrimarySitesResponse(
      Collection<PrimarySiteEntity> primarySiteEntities) {
    return ListPrimarySitesResponse.newBuilder()
        .addAllPrimarySites(
            primarySiteEntities.stream()
                .map(this::primarySiteEntityToPrimarySite)
                .collect(toList()))
        .build();
  }

  default ListCountriesResponse countryEntitiesToListCountriesResponse(
      Collection<CountryEntity> countryEntities) {
    return ListCountriesResponse.newBuilder()
        .addAllCountries(
            countryEntities.stream().map(this::countryEntityToCountry).collect(toList()))
        .build();
  }

  default ListRegionsResponse regionEntitiesToListRegionsResponse(
      Collection<RegionEntity> regionEntities) {
    return ListRegionsResponse.newBuilder()
        .addAllRegions(regionEntities.stream().map(this::regionEntityToRegion).collect(toList()))
        .build();
  }

  default ListInstitutionsResponse institutionEntitiesToListInstitutionsResponse(
      Collection<InstitutionEntity> institutionEntities) {
    return ListInstitutionsResponse.newBuilder()
        .addAllInstitutions(
            institutionEntities.stream()
                .map(this::institutionEntityToInstitution)
                .collect(toList()))
        .build();
  }

  default AddInstitutionsResponse institutionsToAddInstitutionsResponse(
      Collection<InstitutionEntity> institutionEntities) {
    return AddInstitutionsResponse.newBuilder()
        .addAllInstitutions(
            institutionEntities.stream()
                .map(this::institutionEntityToInstitution)
                .collect(toList()))
        .build();
  }

  default InviteUserResponse inviteIdToInviteUserResponse(@NonNull UUID inviteId) {
    return InviteUserResponse.newBuilder().setInviteId(StringValue.of(inviteId.toString())).build();
  }

  /** Enum Boxing Converters */
  default MembershipTypeValue boxMembershipType(MembershipType m) {
    return MembershipTypeValue.newBuilder().setValue(m).build();
  }

  default MembershipType unboxMembershipTypeValue(@NonNull MembershipTypeValue v) {
    return v.getValue();
  }

  default InviteStatusValue JoinProgramInviteStatusToInviteStatus(
      JoinProgramInviteEntity.Status status) {
    switch (status) {
      case ACCEPTED:
        return InviteStatusValue.newBuilder().setValue(InviteStatus.ACCEPTED).build();
      case EXPIRED:
        return InviteStatusValue.newBuilder().setValue(InviteStatus.EXPIRED).build();
      case PENDING:
        return InviteStatusValue.newBuilder().setValue(InviteStatus.PENDING).build();
      case INVALID:
        return InviteStatusValue.newBuilder().build();
      case REVOKED:
        return InviteStatusValue.newBuilder().build();
    }
    return InviteStatusValue.newBuilder().build();
  }

  default InviteStatus unboxInviteStatusValue(InviteStatusValue status) {
    return status.getValue();
  }

  default InviteStatusValue boxInviteStatus(InviteStatus status) {
    return InviteStatusValue.newBuilder().setValue(status).build();
  }

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeEmail", ignore = true)
  @Mapping(target = "mergeFirstName", ignore = true)
  @Mapping(target = "mergeLastName", ignore = true)
  @Mapping(target = "mergeRole", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "email", source = "userEmail")
  User JoinProgramInviteToUser(JoinProgramInviteEntity invitation);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeUser", ignore = true)
  @Mapping(target = "mergeStatus", ignore = true)
  @Mapping(target = "mergeAcceptedAt", ignore = true)
  @Mapping(target = "mergeDacoApproved", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "user", source = "invitation")
  @Mapping(target = "dacoApproved", source = "dacoApproved")
  UserDetails joinProgramInviteToUserDetails(
      Integer dummy, JoinProgramInviteEntity invitation, boolean dacoApproved);

  default UserDetails joinProgramInviteToUserDetails(JoinProgramInviteEntity invitation) {
    return joinProgramInviteToUserDetails(0, invitation, false);
  }

  default UserDetails joinProgramInviteToUserDetails(
      JoinProgramInviteEntity invitation, boolean dacoApproved) {
    return joinProgramInviteToUserDetails(0, invitation, dacoApproved);
  }

  default UserDetails userWithOptionalJoinProgramInviteToUserDetails(
      User user, Optional<JoinProgramInviteEntity> invite, boolean dacoApproved) {
    val builder = UserDetails.newBuilder();
    builder.setUser(user).setDacoApproved(BoolValue.of((dacoApproved)));

    if (invite.isEmpty()) {
      return builder.build();
    }

    val status = JoinProgramInviteStatusToInviteStatus(invite.get().getStatus());
    val builder2 = builder.setStatus(boxInviteStatus(status.getValue()));

    if (status.getValue() == InviteStatus.PENDING) {
      return builder2.build();
    }

    val accepted = CommonConverter.INSTANCE.localDateTimeToTimestamp(invite.get().getAcceptedAt());
    if (accepted != null) {
      return builder2.setAcceptedAt(accepted).build();
    }
    return builder2.build();
  }

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "removeUserDetails", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "userDetailsOrBuilderList", ignore = true)
  @Mapping(target = "userDetailsBuilderList", ignore = true)
  @Mapping(target = "userDetailsList", source = "invitations")
  ListUsersResponse invitationsToListUsersResponse(
      Integer dummy, Collection<JoinProgramInviteEntity> invitations);

  default ListUsersResponse invitationsToListUsersResponse(
      Collection<JoinProgramInviteEntity> invitations) {
    return invitationsToListUsersResponse(0, invitations);
  }

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  @Mapping(target = "mergeCreatedAt", ignore = true)
  @Mapping(target = "mergeExpiresAt", ignore = true)
  @Mapping(target = "mergeAcceptedAt", ignore = true)
  @Mapping(target = "mergeProgram", ignore = true)
  @Mapping(target = "mergeEmailSent", ignore = true)
  @Mapping(target = "statusValue", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "user", ignore = true)
  @Mapping(target = "mergeUser", ignore = true)
  JoinProgramInvite joinProgramInviteEntityToJoinProgramInvite(JoinProgramInviteEntity entity);

  @AfterMapping
  default void setUser(
      @NonNull JoinProgramInviteEntity entity,
      @NonNull @MappingTarget JoinProgramInvite.Builder joinProgramInvite) {
    val userRole = UserRoleValue.newBuilder().setValue(entity.getRole());
    val user =
        User.newBuilder()
            .setEmail(StringValue.of(entity.getUserEmail()))
            .setFirstName(StringValue.of(entity.getFirstName()))
            .setLastName(StringValue.of(entity.getLastName()))
            .setRole(userRole)
            .build();
    joinProgramInvite.setUser(user);
  }
}
