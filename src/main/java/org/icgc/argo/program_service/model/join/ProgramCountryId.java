package org.icgc.argo.program_service.model.join;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.enums.SqlFields;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@Embeddable
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramCountryId implements Serializable {

  @Column(name = SqlFields.PROGRAMID_JOIN)
  private UUID programId;

  @Column(name = SqlFields.COUNTRYID_JOIN)
  private UUID countryId;

}
