package org.icgc.argo.program_service.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Program {

  private String short_name;
  private String description;
  private String name;
  private MembershipTypeValueDTO membership_type;
  private int commitment_donors;
  private int submitted_donors;
  private int genomic_donors;
  private String website;
  private List<String> cancer_types;
  private List<String> primary_sites;
  private List<String> institutions;
  private List<String> countries;
}
