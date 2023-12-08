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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.icgc.argo.program_service.model.dto.*;
import org.icgc.argo.program_service.model.exceptions.ProgramRuntimeException;
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
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
    return (T) builder.build();
  }

  public String getJsonFromObject(Object response) {
    try {
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
    } catch (Exception e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
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

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
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

      updateProgramResponseDTO =
          objectMapper.readValue(responseJson, UpdateProgramResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return updateProgramResponseDTO;
  }

  public ProgramDetailsDTO prepareGetProgramResponse(GetProgramResponse response) {

    ProgramDetailsDTO programDetailsDTO = new ProgramDetailsDTO();
    try {
      String responseJson = JsonFormat.printer().print(response.getProgram());
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      programDetailsDTO = objectMapper.readValue(responseJson, ProgramDetailsDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return programDetailsDTO;
  }

  public ProgramDetailsDTO prepareGetProgramResponse(ProgramDetails programDetails) {

    ProgramDetailsDTO programDetailsDTO = new ProgramDetailsDTO();
    ProgramsDTO programsDTO = new ProgramsDTO();
    programsDTO.setCancerTypes(programDetails.getProgram().getCancerTypesList());
    programsDTO.setCountries(programDetails.getProgram().getCountriesList());
    programsDTO.setCommitmentDonors(programDetails.getProgram().getCommitmentDonors().getValue());
    programsDTO.setDescription(programDetails.getProgram().getDescription().getValue());
    programsDTO.setGenomicDonors(programDetails.getProgram().getGenomicDonors().getValue());
    programsDTO.setInstitutions(programDetails.getProgram().getInstitutionsList());
    programsDTO.setCountries(programDetails.getProgram().getCountriesList());
    programsDTO.setName(programDetails.getProgram().getName().getValue());
    programsDTO.setMembershipType(programDetails.getProgram().getMembershipType().getValue());
    programsDTO.setPrimarySites(programDetails.getProgram().getPrimarySitesList());
    programsDTO.setShortName(programDetails.getProgram().getShortName().getValue());
    programsDTO.setSubmittedDonors(programDetails.getProgram().getSubmittedDonors().getValue());
    programsDTO.setWebsite(programDetails.getProgram().getWebsite().getValue());
    programDetailsDTO.setProgram(programsDTO);
    LegacyDetailsDTO legacyDetailsDTO = new LegacyDetailsDTO();
    legacyDetailsDTO.setLegacyShortName(programDetails.getLegacy().getLegacyShortName().getValue());
    programDetailsDTO.setLegacy(legacyDetailsDTO);
    MetadataDTO metadataDTO = new MetadataDTO();
    metadataDTO.setCreatedAt(
        String.valueOf(programDetails.getMetadata().getCreatedAt().getSeconds()));
    metadataDTO.setUpdatedAt(
        String.valueOf(programDetails.getMetadata().getUpdatedAt().getSeconds()));
    programDetailsDTO.setMetadata(metadataDTO);
    return programDetailsDTO;
  }

  public List<ProgramDetailsDTO> prepareListProgramsResponse(ListProgramsResponse response) {

    ProgramsResponseDTO programsResponseDTO = new ProgramsResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      programsResponseDTO = objectMapper.readValue(responseJson, ProgramsResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return programsResponseDTO.getPrograms();
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

      inviteUserResponseDTO = objectMapper.readValue(responseJson, InviteUserResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
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

      joinProgramResponseDTO = objectMapper.readValue(responseJson, JoinProgramResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
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

      listUsersResponseDTO = objectMapper.readValue(responseJson, ListUsersResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
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

      removeUserResponseDTO = objectMapper.readValue(responseJson, RemoveUserResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return removeUserResponseDTO;
  }

  public JoinProgramInviteDTO prepareGetJoinProgramInviteResponse(JoinProgramInvite response) {

    JoinProgramInviteDTO joinProgramInviteDTO = new JoinProgramInviteDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      joinProgramInviteDTO = objectMapper.readValue(responseJson, JoinProgramInviteDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return joinProgramInviteDTO;
  }

  public ListCancersResponseDTO prepareListCancersResponse(ListCancersResponse response) {

    ListCancersResponseDTO listCancersResponseDTO = new ListCancersResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      listCancersResponseDTO = objectMapper.readValue(responseJson, ListCancersResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return listCancersResponseDTO;
  }

  public ListPrimarySitesResponseDTO prepareListPrimarySitesResponse(
      ListPrimarySitesResponse response) {

    ListPrimarySitesResponseDTO listPrimarySitesResponseDTO = new ListPrimarySitesResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      listPrimarySitesResponseDTO =
          objectMapper.readValue(responseJson, ListPrimarySitesResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return listPrimarySitesResponseDTO;
  }

  public ListCountriesResponseDTO prepareListCountriesResponse(ListCountriesResponse response) {

    ListCountriesResponseDTO listCountriesResponseDTO = new ListCountriesResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      listCountriesResponseDTO =
          objectMapper.readValue(responseJson, ListCountriesResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return listCountriesResponseDTO;
  }

  public ListRegionsResponseDTO prepareListRegionsResponse(ListRegionsResponse response) {

    ListRegionsResponseDTO listRegionsResponseDTO = new ListRegionsResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      listRegionsResponseDTO = objectMapper.readValue(responseJson, ListRegionsResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return listRegionsResponseDTO;
  }

  public ListInstitutionsResponseDTO prepareListInstitutionsResponse(
      ListInstitutionsResponse response) {

    ListInstitutionsResponseDTO listInstitutionsResponseDTO = new ListInstitutionsResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      listInstitutionsResponseDTO =
          objectMapper.readValue(responseJson, ListInstitutionsResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return listInstitutionsResponseDTO;
  }

  public AddInstitutionsResponseDTO prepareAddInstitutionsResponse(
      AddInstitutionsResponse response) {

    AddInstitutionsResponseDTO addInstitutionsResponseDTO = new AddInstitutionsResponseDTO();
    try {
      String responseJson = JsonFormat.printer().print(response);
      objectMapper =
          JsonMapper.builder()
              .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
              .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
              .build();

      addInstitutionsResponseDTO =
          objectMapper.readValue(responseJson, AddInstitutionsResponseDTO.class);

    } catch (JsonProcessingException | InvalidProtocolBufferException e) {
      log.error(ExceptionUtils.getStackTrace(e));
      throw new ProgramRuntimeException(e.getMessage());
    }
    return addInstitutionsResponseDTO;
  }
}
