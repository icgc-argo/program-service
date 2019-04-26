package org.icgc.argo.program_service.grpc;

import com.google.common.collect.Streams;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.Empty;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.CreateProgramRequest;
import org.icgc.argo.program_service.CreateProgramResponse;
import org.icgc.argo.program_service.ListProgramsResponse;
import org.icgc.argo.program_service.mappers.ProgramMapper;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.stream.Collectors;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {
  private final ProgramRepository programRepository;
  private final ProgramMapper programMapper;

  @Autowired
  public ProgramServiceImpl(ProgramRepository programRepository, ProgramMapper programMapper) {
    this.programRepository = programRepository;
    this.programMapper = programMapper;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    val program = request.getProgram();
    val dao = programMapper.ProgramToProgramEntity(program);
    val today = LocalDate.now();
    dao.setCreatedAt(today);
    dao.setUpdatedAt(today);

    val entity = programRepository.save(dao);
    val response = CreateProgramResponse
      .newBuilder()
      .setId(programMapper.map(entity.getId()))
      .setCreatedAt(programMapper.map(entity.getCreatedAt()))
      .build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val programs = programRepository.findAll();

    val results =
      Streams.stream(programs)
      .map(programMapper::map)
      .collect(Collectors.toUnmodifiableList());

    val collection = ListProgramsResponse
      .newBuilder()
      .addAllPrograms(results)
      .build();

    responseObserver.onNext(collection);
    responseObserver.onCompleted();
  }
}
