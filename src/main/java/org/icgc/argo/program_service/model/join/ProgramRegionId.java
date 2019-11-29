package org.icgc.argo.program_service.model.join;

import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.icgc.argo.program_service.model.enums.SqlFields;

@Data
@Builder
@Embeddable
@FieldNameConstants
@NoArgsConstructor
@AllArgsConstructor
public class ProgramRegionId implements Serializable {

  @Column(name = SqlFields.PROGRAMID_JOIN)
  private UUID programId;

  @Column(name = SqlFields.REGIONID_JOIN)
  private UUID regionId;
}
