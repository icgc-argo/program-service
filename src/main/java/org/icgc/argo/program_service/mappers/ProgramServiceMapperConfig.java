package org.icgc.argo.program_service.mappers;

import org.mapstruct.*;

@MapperConfig(
  unmappedTargetPolicy = ReportingPolicy.ERROR,
  injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  mappingInheritanceStrategy =  MappingInheritanceStrategy.AUTO_INHERIT_ALL_FROM_CONFIG
)
public class ProgramServiceMapperConfig {
}
