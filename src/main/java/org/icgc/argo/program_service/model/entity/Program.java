package org.icgc.argo.program_service.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "program")
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Program {

  @Id
  @GenericGenerator(name = "program_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "program_uuid")
  private UUID id;

  private String shortName;

  private String name;

  private String description;

  private String membershipType;

  private int commitmentDonors;

  private int submittedDonors;

  private int genomicDonors;

  private String website;

  private Date dateCreated;

}
