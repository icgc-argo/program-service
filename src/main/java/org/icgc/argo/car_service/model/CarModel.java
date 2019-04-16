package org.icgc.argo.car_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarModel {

  private UUID id;
  private String brand;
  private String model;
  private DriveType type;
  private int year;
  private int horsepower;
  private boolean electric;

}
