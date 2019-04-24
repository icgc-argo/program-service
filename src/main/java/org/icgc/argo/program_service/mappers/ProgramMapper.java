package org.icgc.argo.program_service.mappers;

import com.google.protobuf.Timestamp;
import org.icgc.argo.program_service.model.entity.Program;
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
    Program ProgramMessageToProgram(org.icgc.argo.program_service.Program programMessage);

    org.icgc.argo.program_service.Program ProgramToProgramMessage(Program program);

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
