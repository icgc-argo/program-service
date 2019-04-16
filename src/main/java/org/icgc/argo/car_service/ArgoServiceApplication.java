package org.icgc.argo.car_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class ArgoServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ArgoServiceApplication.class, args);
  }

}
