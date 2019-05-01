package org.icgc.argo.program_service.mappers;

import com.google.protobuf.Timestamp;
import org.icgc.argo.program_service.GetProgramResponse;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  public interface ProgramMapper {
    /**
     * Program Conversions
     */
    Program ProgramEntityToProgram(ProgramEntity entity);
    ProgramEntity ProgramToProgramEntity(Program program);

    default GetProgramResponse toGetProgramResponse(ProgramEntity entity) {
      return GetProgramResponse
        .newBuilder()
        .setId(entity.getId().toString())
        .setCreatedAt(map(entity.getCreatedAt()))
        .setUpdatedAt(map(entity.getUpdatedAt()))
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

    default String map(UUID uuid) {
      return uuid.toString();
    }

    default LocalDateTime map(Timestamp timestamp) {
      return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()), ZoneId.of("UTC"));
    }

    default Timestamp map(LocalDateTime dateTime) {
      Instant instant = dateTime.toInstant(ZoneOffset.UTC);
      return Timestamp.newBuilder()
              .setSeconds(instant.getEpochSecond())
              .setNanos(instant.getNano())
              .build();
    }

}
