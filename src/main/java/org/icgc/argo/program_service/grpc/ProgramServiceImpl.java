package org.icgc.argo.program_service.grpc;

import com.google.common.collect.Streams;
import lombok.val;
import org.icgc.argo.program_service.ProgramCollection;
import org.icgc.argo.program_service.ProgramDetails;
import org.icgc.argo.program_service.ProgramServiceGrpc;
import org.icgc.argo.program_service.converters.DaoDToConverter;
import org.icgc.argo.program_service.repository.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor.EgoAuth;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProgramServiceImpl extends ProgramServiceGrpc.ProgramServiceImplBase {
  private final ProgramRepository programRepository;
  private final DaoDToConverter converter;
  @Autowired
  public ProgramServiceImpl(ProgramRepository programRepository, DaoDToConverter converter) {
    this.programRepository = programRepository;
    this.converter = converter;
  }

  @Override
  @EgoAuth(typesAllowed = {"ADMIN"})
  public void create(
    // TODO: (1) Create the rest of the program entities
    //       (2) Set up the permissions, groups in EGO
    //       (3) Populate the lookup tables for program, role, group_id
    ProgramDetails request, io.grpc.stub.StreamObserver<ProgramDetails> responseObserver) {
    val program = request.getProgram();
    val dao = converter.convertProgramToDao(program);
    programRepository.save(dao);
    responseObserver.onNext(request);
    responseObserver.onCompleted();
  }

  @Override
  public void list(org.icgc.argo.program_service.Empty request,
    io.grpc.stub.StreamObserver<org.icgc.argo.program_service.ProgramCollection> responseObserver) {
    val programs = programRepository.findAll();

    val results = Streams.stream(programs)
      .map(p-> converter.convertDaoToProgram(p))
      .collect(Collectors.toUnmodifiableList());

    val collection = ProgramCollection
      .newBuilder()
      .addAllPrograms(results)
      .build();
    responseObserver.onNext(collection);
  }
}
