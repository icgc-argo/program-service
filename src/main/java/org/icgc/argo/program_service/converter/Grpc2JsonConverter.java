package org.icgc.argo.program_service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.protobuf.AbstractMessage.Builder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.icgc.argo.program_service.model.dto.CreateProgramResponseDTO;
import org.icgc.argo.program_service.proto.CreateProgramResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Grpc2JsonConverter {

  @Autowired private ObjectMapper objectMapper;

  public static <T extends Message> T fromJson(String json, Class<T> clazz) throws IOException {
    Builder builder = null;
    try {
      builder = (Builder) clazz.getMethod("newBuilder").invoke(null);

    } catch (IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException e) {
      e.printStackTrace();
      return null;
    }
    JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
    return (T) builder.build();
  }

  public String getJsonFromObject(Object response) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    } catch (Exception e) {
      log.error(ExceptionUtils.getStackTrace(e));
      return null;
    }
  }

  public CreateProgramResponseDTO prepareCreateProgramResponse(CreateProgramResponse response) {

    CreateProgramResponseDTO createProgramResponseDTO = new CreateProgramResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .build();
      createProgramResponseDTO =
          objectMapper.readValue(responseJson, CreateProgramResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return createProgramResponseDTO;
  }
}
