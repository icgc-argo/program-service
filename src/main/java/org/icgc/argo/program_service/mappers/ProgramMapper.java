package org.icgc.argo.program_service.mappers;

import com.google.protobuf.Timestamp;
import org.icgc.argo.program_service.GetProgramResponse;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@Mapper(config = ProgramServiceMapperConfig.class)
  public interface ProgramMapper {
    @Mapping(target="mergeFrom", ignore = true)
    @Mapping(target="clearField", ignore = true)
    @Mapping(target="clearOneof", ignore = true)
    @Mapping(target="shortNameBytes", ignore = true)
    @Mapping(target="descriptionBytes", ignore = true)
    @Mapping(target="nameBytes", ignore = true)
    @Mapping(target="membershipTypeValue", ignore = true)
    @Mapping(target="websiteBytes", ignore = true)
    @Mapping(target="institutionsBytes", ignore = true)
    @Mapping(target="countriesBytes", ignore = true)
    @Mapping(target="regionsBytes", ignore = true)
    @Mapping(target="unknownFields", ignore = true)
    @Mapping(target="mergeUnknownFields", ignore = true)
    @Mapping(target="allFields", ignore=true)
    @Mapping(target="cancerTypesValueList", ignore = true)
    @Mapping(target="primarySitesValueList", ignore = true)
    Program ProgramEntityToProgram(ProgramEntity entity);

    @Mapping(target="id", ignore = true)
    @Mapping(target="createdAt", ignore = true)
    @Mapping(target="updatedAt", ignore = true)
    ProgramEntity ProgramToProgramEntity(Program program);

    default GetProgramResponse toGetProgramResponse(ProgramEntity entity) {
      return GetProgramResponse
        .newBuilder()
        .setId(entity.getId().toString())
        .setCreatedAt(toTimeStamp(entity.getCreatedAt()))
        .setUpdatedAt(toTimeStamp(entity.getUpdatedAt()))
        .setProgram(ProgramEntityToProgram(entity))
        .build();
    }

    default UUID toUUID(String s) {
      try {
        return UUID.fromString(s);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    default String toString(UUID uuid) {
      return uuid.toString();
    }

    default LocalDateTime toLocalDateTime(Timestamp timestamp) {
      return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()), ZoneId.of("UTC"));
    }

    default Timestamp toTimeStamp(LocalDateTime dateTime) {
      Instant instant = dateTime.toInstant(ZoneOffset.UTC);
      return Timestamp.newBuilder()
              .setSeconds(instant.getEpochSecond())
              .setNanos(instant.getNano())
              .build();
    }

}
