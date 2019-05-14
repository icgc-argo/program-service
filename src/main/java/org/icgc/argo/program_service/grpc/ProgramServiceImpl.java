package org.icgc.argo.program_service.grpc;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.CreateProgramRequest;
import org.icgc.argo.program_service.CreateProgramResponse;
import org.icgc.argo.program_service.ListProgramsResponse;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.converter.ToEntityProgramConverter;
import org.icgc.argo.program_service.converter.ToProtoProgramConverter;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.icgc.argo.program_service.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {
  private final ProgramRepository programRepository;
  private final ToProtoProgramConverter toProtoProgramConverter;
  private final ToEntityProgramConverter toEntityProgramConverter;

  @Autowired
  public ProgramServiceImpl(@NonNull ProgramRepository programRepository,
     @NonNull ToProtoProgramConverter toProtoProgramConverter,
      @NonNull ToEntityProgramConverter toEntityProgramConverter ) {
    this.programRepository = programRepository;
    this.toProtoProgramConverter = toProtoProgramConverter;
    this.toEntityProgramConverter = toEntityProgramConverter;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void createProgram(CreateProgramRequest request, StreamObserver<CreateProgramResponse> responseObserver) {
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    val program = request.getProgram();
    val dao = toEntityProgramConverter.programToProgramEntity(program);
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    dao.setCreatedAt(now);
    dao.setUpdatedAt(now);

    val entity = programRepository.save(dao);
    val response = toProtoProgramConverter.programEntityToCreateProgramResponse(entity);
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void listPrograms(Empty request, StreamObserver<ListProgramsResponse> responseObserver) {
    val programEntities = ImmutableList.copyOf(programRepository.findAll());
    val listProgramsResponse = toProtoProgramConverter.programEntitiesToListProgramsResponse(programEntities);
    responseObserver.onNext(listProgramsResponse);
    responseObserver.onCompleted();
  }
}
