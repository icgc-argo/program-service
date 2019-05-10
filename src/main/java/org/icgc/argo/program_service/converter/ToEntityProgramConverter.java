package org.icgc.argo.program_service.converter;

import org.icgc.argo.program_service.MembershipType;
import org.icgc.argo.program_service.MembershipTypeValue;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(config = ConverterConfig.class,
    uses = { CommonConverter.class },
    unmappedTargetPolicy = ReportingPolicy.WARN,
    unmappedSourcePolicy = ReportingPolicy.ERROR )
public interface ToEntityProgramConverter {

  ProgramEntity map(Program p);

  default MembershipType unboxMembershipTypeValue(MembershipTypeValue v){
    return v.getValue();
  }


}
