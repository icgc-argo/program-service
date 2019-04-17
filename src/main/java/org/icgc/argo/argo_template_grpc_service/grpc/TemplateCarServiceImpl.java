package org.icgc.argo.argo_template_grpc_service.grpc;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.argo_template_grpc_service.model.CarModel;
import org.icgc.argo.argo_template_grpc_service.model.DriveType;
import org.icgc.argo.proto.template_car_service.TemplateCar;
import org.icgc.argo.proto.template_car_service.TemplateCarRequest;
import org.icgc.argo.proto.template_car_service.TemplateCarServiceGrpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class TemplateCarServiceImpl extends TemplateCarServiceGrpc.TemplateCarServiceImplBase{

  @Autowired
  public TemplateCarServiceImpl() {
  }

  @Override public void create(TemplateCar request, StreamObserver<TemplateCar> responseObserver) {
    log.info("Storing car: {}", request.toString());

    val carDao = CarModel.builder()
        .id(UUID.fromString(request.getId()))
        .brand(request.getBrand())
        .year(request.getDateCreated().getYear())
        .electric(request.getElectric())
        .horsepower(request.getHorsepower())
        .model(request.getModel())
        .type(DriveType.resolveDriveType(request.getType().name()))
        .build();

    // do something with carDao

    responseObserver.onNext(request);
    responseObserver.onCompleted();
  }

  @Override public void get(TemplateCarRequest request, StreamObserver<TemplateCar> responseObserver) {
    log.info("Reading the car for id: {}", request.getId());
    val dummyCar = TemplateCar.newBuilder()
        .setId(request.getId())
        .build();
    responseObserver.onNext(dummyCar);
    responseObserver.onCompleted();
  }


}
