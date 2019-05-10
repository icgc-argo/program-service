package org.icgc.argo.program_service.converter;

import com.google.protobuf.BoolValue;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

@Mapper(config = ConverterConfig.class)
public interface CommonConverter {

  default String unboxStringValue(StringValue v){
    return v.getValue();
  }

  default StringValue boxString(String s){
    return StringValue.of(s);
  }

  default int unboxInt32Value(Int32Value v){
    return v.getValue();
  }

  default Int32Value boxInt(int i){
    return Int32Value.of(i);
  }

  default boolean unboxBoolValue(BoolValue v){
    return v.getValue();
  }

  default BoolValue boxBoolean(boolean v){
    return BoolValue.of(v);
  }

  default UUID stringToUUID(String s) {
    try {
      return UUID.fromString(s);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  default String uuidToString(UUID uuid) {
    return uuid.toString();
  }

  default LocalDateTime timestampToLocalDateTime(Timestamp timestamp) {
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()), ZoneId.of("UTC"));
  }

  default Timestamp localDateTimeToTimestamp(LocalDateTime dateTime) {
    Instant instant = dateTime.toInstant(ZoneOffset.UTC);
    return Timestamp.newBuilder()
        .setSeconds(instant.getEpochSecond())
        .setNanos(instant.getNano())
        .build();
  }

}
