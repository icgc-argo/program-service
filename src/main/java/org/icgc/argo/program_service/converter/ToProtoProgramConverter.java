package org.icgc.argo.program_service.converter;

import org.icgc.argo.program_service.Cancer;
import org.icgc.argo.program_service.MembershipType;
import org.icgc.argo.program_service.MembershipTypeValue;
import org.icgc.argo.program_service.PrimarySite;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = ConverterConfig.class, uses = { CommonConverter.class })
public interface ToProtoProgramConverter {

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


  /**
   * JoinEntity Converters
   */
  //TODO [rtisma]: what is the mapstruct way of doing this?
  default CancerEntity programCancerToCancerEntity(ProgramCancer c){
    return c.getCancer();
  }

  default PrimarySiteEntity programPrimarySiteToPrimarySiteEntity(ProgramPrimarySite c){
    return c.getSite();
  }

  /**
   *  Enum Boxing Converters
   */
  default MembershipTypeValue boxMembershipType(MembershipType m){
    return MembershipTypeValue.newBuilder().setValue(m).build();
  }

}
