package org.icgc.argo.program_service.mappers;

import com.google.protobuf.MessageOrBuilder;
import org.mapstruct.*;

@org.mapstruct.MapperConfig(
  unmappedTargetPolicy = ReportingPolicy.ERROR,
  injectionStrategy = InjectionStrategy.CONSTRUCTOR,
  nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
  collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
  mappingInheritanceStrategy =  MappingInheritanceStrategy.AUTO_INHERIT_ALL_FROM_CONFIG
)
public interface MapperConfig {
  @Mapping(target = "unknownFields", ignore = true)
  @Mapping(target = "allFields", ignore = true)
  MessageOrBuilder configForProtoMessageOrBuilder(Object o);
}
