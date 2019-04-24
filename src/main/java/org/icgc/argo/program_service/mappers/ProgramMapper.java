package org.icgc.argo.program_service.mappers;

import com.google.protobuf.Timestamp;
import org.icgc.argo.program_service.model.entity.Program;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@Mapper( config = MapperConfig.class)
  public interface ProgramMapper {
    /**
     * Program Conversions
     */
    Program convertProgramToDao(org.icgc.argo.program_service.Program program);
    @Mapping(target = "mergeFrom", ignore = true)
    @Mapping(target = "clearField", ignore = true)
    @Mapping(target = "clearOneof", ignore = true)
    @Mapping(target = "unknownFields", ignore = true)
    @Mapping(target = "mergeUnknownFields", ignore = true)
    @Mapping(target = "allFields", ignore = true)
    @Mapping(target = "idBytes", ignore = true)
    @Mapping(target = "shortNameBytes", ignore = true)
    @Mapping(target = "descriptionBytes", ignore = true)
    @Mapping(target = "nameBytes", ignore = true)
    @Mapping(target = "membershipTypeValue", ignore = true)
    @Mapping(target = "websiteBytes", ignore = true)
    @Mapping(target = "mergeCreatedAt", ignore = true)
    org.icgc.argo.program_service.Program convertDaoToProgram(Program dao);

    default UUID map(String s) {
      return UUID.fromString(s);
    }

    default String map(UUID uuid) {
      return uuid.toString();
    }

    default LocalDateTime map(Timestamp timestamp) {
      return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()), ZoneId.of("UTC"));
    }

    default Timestamp map(LocalDateTime date) {
      Instant instant = date.toInstant(ZoneOffset.UTC);
      return Timestamp.newBuilder()
              .setSeconds(instant.getEpochSecond())
              .setNanos(instant.getNano())
              .build();
    }

}
