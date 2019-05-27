package org.icgc.argo.program_service.converter;

import com.google.protobuf.StringValue;
import lombok.NonNull;
import org.icgc.argo.program_service.Cancer;
import org.icgc.argo.program_service.CreateProgramResponse;
import org.icgc.argo.program_service.InviteUserResponse;
import org.icgc.argo.program_service.ListProgramsResponse;
import org.icgc.argo.program_service.MembershipType;
import org.icgc.argo.program_service.MembershipTypeValue;
import org.icgc.argo.program_service.PrimarySite;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(config = ConverterConfig.class, uses = { CommonConverter.class })
public interface ProgramConverter {

  /**
   * From Proto Converters
   */

  @Mapping(target = "programCancers", ignore = true)
  CancerEntity cancerToPartialCancerEntity(Cancer c);
  Set<CancerEntity> cancersToCancerEntities(Collection<Cancer> cancers);

  @Mapping(target = "programPrimarySites", ignore = true)
  PrimarySiteEntity primarySiteToPartialPrimarySiteEntity(PrimarySite p);
  Set<PrimarySiteEntity> primarySitesToPrimarySiteEntities(Collection<PrimarySite> primarySites);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "programCancers", ignore = true)
  @Mapping(target = "programPrimarySites", ignore = true)
  @Mapping(target = "egoGroups", ignore = true)
  ProgramEntity programToProgramEntity(Program p);

  @AfterMapping
  default void updateProgramRelationships(@NonNull Program p, @MappingTarget ProgramEntity programEntity){
    cancersToCancerEntities(programToCancers(p))
        .forEach(programEntity::associateCancer);

    primarySitesToPrimarySiteEntities(programToPrimarySites(p))
        .forEach(programEntity::associatePrimarySite);
  }


  /**
   * To Proto Converters
   */
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "mergeShortName", ignore = true)
  @Mapping(target = "mergeDescription", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "mergeMembershipType", ignore = true)
  @Mapping(target = "mergeCommitmentDonors", ignore = true)
  @Mapping(target = "mergeSubmittedDonors", ignore = true)
  @Mapping(target = "mergeGenomicDonors", ignore = true)
  @Mapping(target = "mergeWebsite", ignore = true)
  @Mapping(target = "mergeInstitutions", ignore = true)
  @Mapping(target = "mergeCountries", ignore = true)
  @Mapping(target = "mergeRegions", ignore = true)
  @Mapping(target = "removeCancerTypes", ignore = true)
  @Mapping(target = "removePrimarySites", ignore = true)
  @Mapping(target = "mergeCreatedAt", ignore = true)
  @Mapping(target = "mergeUpdatedAt", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  @Mapping(target = "cancerTypesOrBuilderList", ignore = true)
  @Mapping(target = "cancerTypesBuilderList", ignore = true)
  @Mapping(target = "primarySitesOrBuilderList", ignore = true)
  @Mapping(target = "primarySitesBuilderList", ignore = true)
  @Mapping(source = "programCancers", target = "cancerTypesList")
  @Mapping(source = "programPrimarySites", target = "primarySitesList")
  Program programEntityToProgram(ProgramEntity entity);

  @InheritConfiguration
  List<Program> programEntitiesToPrograms(Collection<ProgramEntity> entities);

	@Mapping(target = "mergeFrom", ignore = true)
	@Mapping(target = "clearField", ignore = true)
	@Mapping(target = "clearOneof", ignore = true)
	@Mapping(target = "mergeId", ignore = true)
	@Mapping(target = "mergeName", ignore = true)
	@Mapping(target = "unknownFields", ignore = true)
	@Mapping(target = "mergeUnknownFields", ignore = true)
	@Mapping(target = "allFields", ignore = true)
  Cancer cancerEntityToCancer(CancerEntity c);

  @Mapping(target = "mergeFrom", ignore = true)
  @Mapping(target = "clearField", ignore = true)
  @Mapping(target = "clearOneof", ignore = true)
  @Mapping(target = "mergeId", ignore = true)
  @Mapping(target = "mergeName", ignore = true)
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "mergeUnknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  PrimarySite primarySiteEntityToPrimarySite(PrimarySiteEntity c);

	@Mapping(target = "mergeFrom", ignore = true)
	@Mapping(target = "clearField", ignore = true)
	@Mapping(target = "clearOneof", ignore = true)
	@Mapping(target = "mergeId", ignore = true)
	@Mapping(target = "mergeCreatedAt", ignore = true)
	@Mapping(target = "unknownFields", ignore = true)
	@Mapping(target = "mergeUnknownFields", ignore = true)
	@Mapping(target = "allFields", ignore = true)
  CreateProgramResponse programEntityToCreateProgramResponse(ProgramEntity p);

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

  default ListProgramsResponse programEntitiesToListProgramsResponse(Collection<ProgramEntity> programEntities){
    return programEntitiesToListProgramsResponse(0, programEntities);
  }

  default InviteUserResponse inviteIdToInviteUserResponse(@NonNull UUID inviteId){
    return InviteUserResponse.newBuilder()
        .setInviteId(StringValue.of(inviteId.toString()))
        .build();
  }

  /**
   * JoinEntity Converters
   */
  //TODO [rtisma]: what is the mapstruct way of doing this?
  default CancerEntity programCancerToCancerEntity(@NonNull ProgramCancer c){
    return c.getCancer();
  }

  default Collection<Cancer> programToCancers(@NonNull Program p){
    return p.getCancerTypesList();
  }

  default PrimarySiteEntity programPrimarySiteToPrimarySiteEntity(@NonNull ProgramPrimarySite c){
    return c.getPrimarySite();
  }

  default Collection<PrimarySite> programToPrimarySites(@NonNull Program p){
    return p.getPrimarySitesList();
  }


  /**
   *  Enum Boxing Converters
   */
  default MembershipTypeValue boxMembershipType(MembershipType m){
    return MembershipTypeValue.newBuilder().setValue(m).build();
  }

  default MembershipType unboxMembershipTypeValue(@NonNull MembershipTypeValue v){
    return v.getValue();
  }



}
