package org.icgc.argo.car_service.grpc;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.car_service.Car;
import org.icgc.argo.car_service.CarRequest;
import org.icgc.argo.car_service.CarServiceGrpc;
import org.icgc.argo.car_service.model.CarModel;
import org.icgc.argo.car_service.model.DriveType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class CarServiceImpl extends CarServiceGrpc.CarServiceImplBase{

  @Autowired
  public CarServiceImpl() {
  }

  @Override public void createCar(Car request, StreamObserver<Car> responseObserver) {
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

  @Override public void getCar(CarRequest request, StreamObserver<Car> responseObserver) {
    log.info("Reading the car for id: {}", request.getId());
    val dummyCar = Car.newBuilder()
        .setId(request.getId())
        .build();
    responseObserver.onNext(dummyCar);
    responseObserver.onCompleted();
  }

}
