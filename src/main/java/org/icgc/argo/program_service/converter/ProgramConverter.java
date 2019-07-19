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

package org.icgc.argo.program_service.converter;

import com.google.protobuf.StringValue;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.*;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Mapper(config = ConverterConfig.class, uses = { CommonConverter.class })
public interface ProgramConverter {
  ProgramConverter INSTANCE = new ProgramConverterImpl(CommonConverter.INSTANCE);

  /**
   * From Proto Converters
   */

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "programCancers", ignore = true)
  @Mapping(target = "programPrimarySites", ignore = true)
  ProgramEntity programToProgramEntity(Program p);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "shortName", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "programCancers", ignore = true)
  @Mapping(target = "programPrimarySites", ignore = true)
  void updateProgram(ProgramEntity updatingProgram, @MappingTarget ProgramEntity programToUpdate);

  /**
   * To Proto Converters
   */
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
  @Mapping(target = "mergeInstitutions", ignore = true)
  @Mapping(target = "mergeCountries", ignore = true)
  @Mapping(target = "mergeRegions", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "cancerTypesList", ignore = true)
  @Mapping(target = "primarySitesList", ignore = true)
  Program programEntityToProgram(ProgramEntity entity);

  @AfterMapping
  default Program updateProgramFromEntity(ProgramEntity entity, Program program) {
    return program.toBuilder().
      addAllCancerTypes(entity.listCancerTypes()).
      addAllPrimarySites(entity.listPrimarySites()).
      build();
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

  default ProgramDetails ProgramEntityToProgramDetails(ProgramEntity value) {
    val p = programEntityToProgram(value);
    val program = updateProgramFromEntity(value, p);
    return ProgramDetails.newBuilder().
      setProgram(program).
      setMetadata(programEntityToMetadata(value)).
      build();
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
  //TODO: [rtisma] this is a hack for a bug in mapstruct when mapping an iterable to a wrapper (non-iterable)
  //  https://github.com/mapstruct/mapstruct/issues/607#issuecomment-309547739
  @Mapping(target = "programsList", source = "programEntities")
  ListProgramsResponse programEntitiesToListProgramsResponse(Integer dummy, Collection<ProgramEntity> programEntities);

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
  EgoUser joinProgramInviteToEgoUser(JoinProgramInvite invite);

  default JoinProgramResponse egoUserToJoinProgramResponse(EgoUser egoUser) {
    return egoUserToJoinProgramResponse(0, egoUser);
  }

  default UserRoleValue boxUserValue(@NonNull UserRole role) {
    return UserRoleValue.newBuilder().setValue(role).build();
  }

  default ListProgramsResponse programEntitiesToListProgramsResponse(Collection<ProgramEntity> programEntities) {
    return programEntitiesToListProgramsResponse(0, programEntities);
  }

  default InviteUserResponse inviteIdToInviteUserResponse(@NonNull UUID inviteId) {
    return InviteUserResponse.newBuilder()
      .setInviteId(StringValue.of(inviteId.toString()))
      .build();
  }

  /**
   * Enum Boxing Converters
   */
  default MembershipTypeValue boxMembershipType(MembershipType m) {
    return MembershipTypeValue.newBuilder().setValue(m).build();
  }

  default MembershipType unboxMembershipTypeValue(@NonNull MembershipTypeValue v) {
    return v.getValue();
  }

  InviteStatus JoinProgramInviteStatusToInviteStatus(JoinProgramInvite.Status status);

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
  User JoinProgramInviteToUser(JoinProgramInvite invitation);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeUser", ignore = true)
  @Mapping(target = "mergeStatus", ignore = true)
  @Mapping(target = "mergeAcceptedAt", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "user", source = "invitation")
  Invitation joinProgramInviteToInvitation(Integer dummy, JoinProgramInvite invitation);

  default Invitation joinProgramInviteToInvitation(JoinProgramInvite invitation) {
    return joinProgramInviteToInvitation(0, invitation);
  }

  default Invitation userWithOptionalJoinProgramInviteToInvitation(User user, Optional<JoinProgramInvite> invite) {
    val builder = Invitation.newBuilder().setUser(user);

    if (invite.isEmpty()) {
      return builder.build();
    }

    val status = JoinProgramInviteStatusToInviteStatus(invite.get().getStatus());
    val builder2 = builder.setStatus(boxInviteStatus(status));

    if (status == InviteStatus.PENDING) {
      return builder2.build();
    }

    val accepted = CommonConverter.INSTANCE.localDateTimeToTimestamp(invite.get().getAcceptedAt());
    return builder2.setAcceptedAt(accepted).build();
  }

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "removeInvitations", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "invitationsOrBuilderList", ignore = true)
  @Mapping(target = "invitationsBuilderList", ignore = true)
  @Mapping(target = "invitationsList", source = "invitations")
  ListUserResponse invitationsToListUserResponse(Integer dummy, Collection<JoinProgramInvite> invitations);

  default ListUserResponse invitationsToListUserResponse(Collection<JoinProgramInvite> invitations) {
    return invitationsToListUserResponse(0, invitations);
  }

}
