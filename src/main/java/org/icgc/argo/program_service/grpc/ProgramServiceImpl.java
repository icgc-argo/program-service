package org.icgc.argo.program_service.grpc;

import com.google.common.collect.Streams;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.CreateProgramRequest;
import org.icgc.argo.program_service.CreateProgramResponse;
import org.icgc.argo.program_service.ListProgramsResponse;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {
  private final ProgramRepository programRepository;
  private final ProgramConverter programConverter;

  @Autowired
  public ProgramServiceImpl(ProgramRepository programRepository, ProgramConverter programConverter) {
    this.programRepository = programRepository;
    this.programConverter = programConverter;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    val program = request.getProgram();
    val dao = programConverter.ProgramToProgramEntity(program);
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    dao.setCreatedAt(now);
    dao.setUpdatedAt(now);

    val entity = programRepository.save(dao);
    val response = CreateProgramResponse
      .newBuilder()
      .setId(programConverter.map(entity.getId()))
      .setCreatedAt(programConverter.map(entity.getCreatedAt()))
      .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val programs = programRepository.findAll();

    val results =
            Streams.stream(programs)
                    .map(programConverter::programEntityToProgram)
                    .collect(Collectors.toUnmodifiableList());

    val collection = ListProgramsResponse
            .newBuilder()
            .addAllPrograms(results)
            .build();

    responseObserver.onNext(collection);
    responseObserver.onCompleted();
  }
}
