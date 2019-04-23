package org.icgc.argo.program_service.converters;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;

import org.icgc.argo.program_service.Date;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.model.entity.ProgramDao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper( config = ConverterConfig.class)
  public interface DaoDToConverter {
    /**
     * Program Conversions
     */
    ProgramDao convertProgramToDao(Program program);
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
    @Mapping(target = "mergeDateCreated", ignore = true)
    Program convertDaoToProgram(ProgramDao dao);

    default UUID map(String s) {
      return UUID.fromString(s);
    }

    default String map(UUID uuid) {
      return uuid.toString();
    }

    default java.util.Date map(Date date) {
      return new java.util.Date(date.getYear(), date.getMonth(), date.getDay());
    }

    default Date map(java.util.Date date) {
      return Date.newBuilder()
        .setYear(date.getYear())
        .setMonth(date.getMonth())
        .setDay(date.getDate())
        .build();
    }
    default String convertToString(StringValue v){
      return v.getValue();
    }

    default int convertToInt(Int32Value v){
      return v.getValue();
    }

    default boolean convertToBoolean(BoolValue v){
      return v.getValue();
    }

    default StringValue convertToStringValue(String v){
      return StringValue.of(v);
    }

    default Int32Value convertToInt32Value(int v){
      return Int32Value.of(v);
    }

    default BoolValue convertToBoolValue(boolean v){
      return BoolValue.of(v);
    }

  }
