package org.icgc.argo.program_service.converter;

import lombok.val;
import org.icgc.argo.program_service.Cancer;
import org.icgc.argo.program_service.MembershipType;
import org.icgc.argo.program_service.MembershipTypeValue;
import org.icgc.argo.program_service.PrimarySite;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Collection;
import java.util.Set;

import static org.icgc.argo.program_service.relationship.Relationships.PROGRAM_CANCER_RELATIONSHIP;
import static org.icgc.argo.program_service.relationship.Relationships.PROGRAM_PRIMARY_SITE_RELATIONSHIP;

@Mapper(config = ConverterConfig.class, uses = { CommonConverter.class } )
public interface ToEntityProgramConverter {

  @Mapping(target = "programCancers", ignore = true)
  CancerEntity cancerToPartialCancerEntity(Cancer c);
  Set<CancerEntity> cancersToCancerEntities(Collection<Cancer> cancers);

  @Mapping(target = "programPrimarySites", ignore = true)
  PrimarySiteEntity primarySiteToPartialPrimarySiteEntity(PrimarySite p);
  Set<PrimarySiteEntity> primarySitesToPrimarySiteEntities(Collection<PrimarySite> primarySites);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "programCancers", ignore = true)
  @Mapping(target = "programPrimarySites", ignore = true)
  ProgramEntity programToProgramEntity(Program p);

  @AfterMapping
  default void updateProgramRelationships(Program p, @MappingTarget ProgramEntity programEntity){
    val cancerEntities = cancersToCancerEntities(programToCancers(p));
    PROGRAM_CANCER_RELATIONSHIP.associate(programEntity, cancerEntities);

    val primarySiteEntities = primarySitesToPrimarySiteEntities(programToPrimarySites(p));
    PROGRAM_PRIMARY_SITE_RELATIONSHIP.associate(programEntity, primarySiteEntities);
  }

  /**
   * Special Conversions
   */
  default MembershipType unboxMembershipTypeValue(MembershipTypeValue v){
    return v.getValue();
  }

  default Collection<Cancer> programToCancers(Program p){
    return p.getCancerTypesList();
  }

  default Collection<PrimarySite> programToPrimarySites(Program p){
    return p.getPrimarySitesList();
  }

}
