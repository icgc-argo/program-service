package org.icgc.argo.program_service.services.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Setter @Getter
public class Context {
  //      public String[] scope;
  User user;

  @JsonIgnoreProperties(ignoreUnknown = true)
  @Setter @Getter
  public static class User {
    String name;
    String email;
    String status;
    String firstName;
    String lastName;
    String test;
    String createdAt;
    String lastLogin;
    String preferredLanguage;
    String type;
    String[] groups;
    String[] permissions;
  }
}
