package org.icgc.argo.program_service.grpc;

import com.google.common.collect.Streams;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.icgc.argo.program_service.Empty;
import org.icgc.argo.program_service.ProgramCollection;
import org.icgc.argo.program_service.ProgramDetails;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.mappers.ProgramMapper;
import org.icgc.argo.program_service.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {
  private final ProgramRepository programRepository;
  private final ProgramMapper converter;

  @Autowired
  public ProgramServiceImpl(ProgramRepository programRepository, ProgramMapper converter) {
    this.programRepository = programRepository;
    this.converter = converter;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void create(ProgramDetails request, StreamObserver<ProgramDetails> responseObserver) {
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    val program = request.getProgram();
    val dao = converter.ProgramMessageToProgram(program);
    programRepository.save(dao);
    responseObserver.onNext(request);
    responseObserver.onCompleted();
  }

  @Override
  public void list(Empty request, StreamObserver<ProgramCollection> responseObserver) {
    val programs = programRepository.findAll();
    val results = Streams.stream(programs)
      .map(converter::ProgramToProgramMessage)
      .collect(Collectors.toUnmodifiableList());

    val collection = ProgramCollection
      .newBuilder()
      .addAllPrograms(results)
      .build();

    responseObserver.onNext(collection);
    responseObserver.onCompleted();
  }
}
