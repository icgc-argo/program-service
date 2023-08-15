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
import org.icgc.argo.program_service.model.dto.*;
import org.icgc.argo.program_service.proto.*;
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

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
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

  public UpdateProgramResponseDTO prepareUpdateProgramResponse(UpdateProgramResponse response) {

    UpdateProgramResponseDTO updateProgramResponseDTO = new UpdateProgramResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .build();

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
      updateProgramResponseDTO =
          objectMapper.readValue(responseJson, UpdateProgramResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return updateProgramResponseDTO;
  }

  public GetProgramResponseDTO prepareGetProgramResponse(GetProgramResponse response) {

    GetProgramResponseDTO getProgramResponseDTO = new GetProgramResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
      getProgramResponseDTO = objectMapper.readValue(responseJson, GetProgramResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return getProgramResponseDTO;
  }

  public ProgramsResponseDTO prepareListProgramsResponse(ListProgramsResponse response) {

    ProgramsResponseDTO programsResponseDTO = new ProgramsResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
      programsResponseDTO = objectMapper.readValue(responseJson, ProgramsResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return programsResponseDTO;
  }

  public InviteUserResponseDTO prepareInviteUserResponse(InviteUserResponse response) {

    InviteUserResponseDTO inviteUserResponseDTO = new InviteUserResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
      inviteUserResponseDTO = objectMapper.readValue(responseJson, InviteUserResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return inviteUserResponseDTO;
  }

  public JoinProgramResponseDTO prepareJoinProgramResponse(JoinProgramResponse response) {

    JoinProgramResponseDTO joinProgramResponseDTO = new JoinProgramResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
      joinProgramResponseDTO = objectMapper.readValue(responseJson, JoinProgramResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return joinProgramResponseDTO;
  }

  public ListUsersResponseDTO prepareListUsersResponse(ListUsersResponse response) {

    ListUsersResponseDTO listUsersResponseDTO = new ListUsersResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
      listUsersResponseDTO = objectMapper.readValue(responseJson, ListUsersResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return listUsersResponseDTO;
  }

  public RemoveUserResponseDTO prepareRemoveUserResponse(RemoveUserResponse response) {

    RemoveUserResponseDTO removeUserResponseDTO = new RemoveUserResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      // JsonNode responseNode = objectMapper.readTree(responseJson).get("createdAt");
      removeUserResponseDTO = objectMapper.readValue(responseJson, RemoveUserResponseDTO.class);

    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InvalidProtocolBufferException e) {
      e.printStackTrace();
    }
    return removeUserResponseDTO;
  }
}
