package org.icgc.argo.program_service.mappers;

import com.google.protobuf.Timestamp;
import org.icgc.argo.program_service.*;

import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.*;
import java.util.UUID;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
  public interface ProgramMapper {
    /**
     * Program Conversions
     */
    Program ProgramEntityToProgram(ProgramEntity entity);
    ProgramEntity ProgramToProgramEntity(Program program);

    default GetProgramResponse map(ProgramEntity entity) {
      return GetProgramResponse
        .newBuilder()
        .setId(map(entity.getId()))
        .setCreatedAt(map(entity.getCreatedAt()))
        .setUpdatedAt(map(entity.getUpdatedAt()))
        .setProgram(ProgramEntityToProgram(entity))
        .build();
    }

    default UUID map(String s) {
      try {
        return UUID.fromString(s);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }

    default String map(UUID uuid) {
      return uuid.toString();
    }

    default LocalDate map(Date date) {
      return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    default Date map (LocalDate date) {
      return Date.newBuilder()
        .setYear(date.getYear())
        .setMonth(date.getMonthValue())
        .setDay(date.getDayOfMonth())
        .build();
    }

    default Timestamp map(LocalDateTime date) {
      Instant instant = date.toInstant(ZoneOffset.UTC);
      return Timestamp.newBuilder()
              .setSeconds(instant.getEpochSecond())
              .setNanos(instant.getNano())
              .build();
    }

}
