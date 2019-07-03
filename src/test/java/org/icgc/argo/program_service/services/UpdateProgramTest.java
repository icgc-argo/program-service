package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.repositories.CancerRepository;
import org.icgc.argo.program_service.repositories.PrimarySiteRepository;
import org.icgc.argo.program_service.repositories.query.CancerSpecification;
import org.icgc.argo.program_service.repositories.query.PrimarySiteSpecification;
import org.icgc.argo.program_service.utils.EntityGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.List;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
public class UpdateProgramTest {

  @Autowired
  private EntityGenerator entityGenerator;

  @Autowired
  private ProgramService programService;

  @Autowired
  private CancerRepository cancerRepository;

  @Autowired
  private PrimarySiteRepository primarySiteRepository;

  @Test
  public void add_cancer_primary_site_success(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    val updatingCancer = entityGenerator.setUpCancer("Soft Tissue cancer");
    val updatingPrimarySite = entityGenerator.setUpPrimarySite("Liver");

    val existingCancers = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val existingPrimarySites = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(existingCancers.isEmpty()).isTrue();
    assertThat(existingPrimarySites.isEmpty()).isTrue();

    val cancers = List.of("Soft Tissue cancer");
    val primarySites = List.of("Liver");
    programService.updateProgram(programToUpdate, cancers, primarySites);

    val updatedCancers = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val updatedPrimarySites = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(updatedCancers.size()).isEqualTo(1);
    assertThat(updatedCancers.contains(updatingCancer)).isTrue();

    assertThat(updatedPrimarySites.size()).isEqualTo(1);
    assertThat(updatedPrimarySites.contains(updatingPrimarySite)).isTrue();
  }

  @Test
  public void dissociate_all_success(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    entityGenerator.setUpCancer("Soft Tissue cancer");
    entityGenerator.setUpCancer("Brain cancer");
    entityGenerator.setUpPrimarySite("Liver");
    entityGenerator.setUpPrimarySite("Brain");

    val cancers = List.of("Soft Tissue cancer", "Brain cancer");
    val primarySites = List.of("Liver", "Brain");
    programService.updateProgram(programToUpdate, cancers, primarySites);

    val cancersBeforeUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySiteBeforeUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersBeforeUpdate.size()).isEqualTo(2);
    assertThat(cancersBeforeUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers)).isTrue();

    assertThat(primarySiteBeforeUpdate.size()).isEqualTo(2);
    assertThat(primarySiteBeforeUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites)).isTrue();

    programService.updateProgram(programToUpdate, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
    val cancersAfterUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySiteAfterUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertTrue(cancersAfterUpdate.isEmpty());
    assertTrue(primarySiteAfterUpdate.isEmpty());
  }

  @Test
  public void remove_cancer_add_primary_site_success(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    entityGenerator.setUpCancer("Liver cancer");
    entityGenerator.setUpCancer("Brain cancer");
    entityGenerator.setUpCancer("Skin cancer");

    entityGenerator.setUpPrimarySite("Liver");
    entityGenerator.setUpPrimarySite("Brain");
    entityGenerator.setUpPrimarySite("Skin");

    val cancers = List.of("Liver cancer", "Brain cancer", "Skin cancer");
    val primarySites = List.of("Liver");
    programService.updateProgram(programToUpdate, cancers, primarySites);

    val cancersBeforeUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySitesBeforeUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersBeforeUpdate.size()).isEqualTo(3);
    assertThat(cancersBeforeUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers)).isTrue();

    assertThat(primarySitesBeforeUpdate.size()).isEqualTo(1);
    assertThat(primarySitesBeforeUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites)).isTrue();

    val cancers2 = List.of("Liver cancer");
    val primarySites2 = List.of("Liver", "Brain", "Skin");
    programService.updateProgram(programToUpdate, cancers2, primarySites2);

    val cancersAfterUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySitesAfterUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersAfterUpdate.size()).isEqualTo(1);
    assertThat(cancersAfterUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers2)).isTrue();

    assertThat(primarySitesAfterUpdate.size()).isEqualTo(3);
    assertThat(primarySitesAfterUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites2)).isTrue();
  }

}
