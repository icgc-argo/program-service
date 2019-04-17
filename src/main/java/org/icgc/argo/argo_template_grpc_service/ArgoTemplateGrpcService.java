package org.icgc.argo.argo_template_grpc_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class ArgoTemplateGrpcService {

  public static void main(String[] args) {
    SpringApplication.run(ArgoTemplateGrpcService.class, args);
  }

}
