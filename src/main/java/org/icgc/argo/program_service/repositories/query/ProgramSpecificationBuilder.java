package org.icgc.argo.program_service.repositories.query;

import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;

import javax.persistence.criteria.Root;
import java.util.UUID;

import static javax.persistence.criteria.JoinType.LEFT;

@Setter
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ProgramSpecificationBuilder extends AbstractSpecificationBuilder<ProgramEntity, UUID> {

  private boolean fetchCancers;

  private boolean fetchPrimarySites;

  @Override
  protected Root<ProgramEntity> setupFetchStrategy(Root<ProgramEntity> root) {
    if (fetchCancers){
      root.fetch(ProgramEntity.Fields.programCancers, LEFT)
          .fetch(ProgramCancer.Fields.cancer, LEFT);
    }
    if (fetchPrimarySites){
      root.fetch(ProgramEntity.Fields.programPrimarySites, LEFT)
          .fetch(ProgramPrimarySite.Fields.primarySite, LEFT);
    }
    return root;
  }
}
