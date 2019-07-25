package org.icgc.argo.program_service.services;

import lombok.val;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.repositories.*;
import org.icgc.argo.program_service.repositories.query.*;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  @Autowired
  private InstitutionRepository institutionRepository;

  @Autowired
  private RegionRepository regionRepository;

  @Autowired
  private CountryRepository countryRepository;

  @Test
  public void add_valid_cancer_valid_primary_site_success(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    val updatingCancer = entityGenerator.setUpCancer("Soft Tissue cancer");
    val updatingPrimarySite = entityGenerator.setUpPrimarySite("Liver");
    val updatingInstitution = entityGenerator.setUpInstitution("OICR");
    val updatingCountry = entityGenerator.setUpCountry("CA");
    val updatingRegion = entityGenerator.setUpRegion("NORTH AMERICA");

    val existingCancers = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val existingPrimarySites = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));
    val existingInstitutions = institutionRepository.findAll(InstitutionSpecification.containsProgram(programId));
    val existingRegions = regionRepository.findAll(RegionSpecification.containsProgram(programId));
    val existingCountries = countryRepository.findAll(CountrySpecification.containsProgram(programId));

    assertThat(existingCancers.isEmpty()).isTrue();
    assertThat(existingPrimarySites.isEmpty()).isTrue();
    assertThat(existingInstitutions.isEmpty());
    assertThat(existingCountries.isEmpty());
    assertThat(existingRegions.isEmpty());

    val cancers = List.of("Soft Tissue cancer");
    val primarySites = List.of("Liver");
    val institutions = List.of("OICR");
    val regions = List.of("NORTH AMERICA");
    val countries = List.of("CA");

    programService.updateProgram(programToUpdate, cancers, primarySites, institutions, countries, regions);

    val updatedCancers = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val updatedPrimarySites = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));
    val updatedInstitutions = institutionRepository.findAll(InstitutionSpecification.containsProgram(programId));
    val updatedCountries = countryRepository.findAll(CountrySpecification.containsProgram(programId));
    val updatedRegions = regionRepository.findAll(RegionSpecification.containsProgram(programId));

    assertThat(updatedCancers.size()).isEqualTo(1);
    assertThat(updatedCancers.contains(updatingCancer)).isTrue();

    assertThat(updatedPrimarySites.size()).isEqualTo(1);
    assertThat(updatedPrimarySites.contains(updatingPrimarySite)).isTrue();

    assertThat(updatedInstitutions.size()).isEqualTo(1);
    assertThat(updatedInstitutions.contains(updatingInstitution)).isTrue();

    assertThat(updatedCountries.size()).isEqualTo(1);
    assertThat(updatedCountries.contains(updatingCountry)).isTrue();

    assertThat(updatedRegions.size()).isEqualTo(1);
    assertThat(updatedRegions.contains(updatingRegion)).isTrue();
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

    entityGenerator.setUpInstitution("OICR");
    entityGenerator.setUpCountry("CA");
    entityGenerator.setUpRegion("NORTH AMERICA");

    val cancers = List.of("Liver cancer", "Brain cancer", "Skin cancer");
    val primarySites = List.of("Liver");
    val institutions = List.of("OICR");
    val regions = List.of("NORTH AMERICA");
    val countries = List.of("CA");
    programService.updateProgram(programToUpdate, cancers, primarySites, institutions, countries, regions);

    val cancersBeforeUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySitesBeforeUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersBeforeUpdate.size()).isEqualTo(3);
    assertThat(cancersBeforeUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers)).isTrue();

    assertThat(primarySitesBeforeUpdate.size()).isEqualTo(1);
    assertThat(primarySitesBeforeUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites)).isTrue();

    val cancers2 = List.of("Liver cancer");
    val primarySites2 = List.of("Liver", "Brain", "Skin");
    programService.updateProgram(programToUpdate, cancers2, primarySites2, institutions, countries, regions);

    val cancersAfterUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySitesAfterUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersAfterUpdate.size()).isEqualTo(1);
    assertThat(cancersAfterUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers2)).isTrue();

    assertThat(primarySitesAfterUpdate.size()).isEqualTo(3);
    assertThat(primarySitesAfterUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites2)).isTrue();
  }

  @Test
  public void associate_empty_cancer_primary_site_fail(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    entityGenerator.setUpCancer("Soft Tissue cancer");
    entityGenerator.setUpCancer("Brain cancer");
    entityGenerator.setUpPrimarySite("Liver");
    entityGenerator.setUpPrimarySite("Brain");
    entityGenerator.setUpInstitution("OICR");
    entityGenerator.setUpCountry("CA");
    entityGenerator.setUpRegion("NORTH AMERICA");

    val cancers = List.of("Soft Tissue cancer", "Brain cancer");
    val primarySites = List.of("Liver", "Brain");
    val institutions = List.of("OICR");
    val regions = List.of("NORTH AMERICA");
    val countries = List.of("CA");
    programService.updateProgram(programToUpdate, cancers, primarySites, institutions, countries, regions);

    val cancersBeforeUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySiteBeforeUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersBeforeUpdate.size()).isEqualTo(2);
    assertThat(cancersBeforeUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers)).isTrue();

    assertThat(primarySiteBeforeUpdate.size()).isEqualTo(2);
    assertThat(primarySiteBeforeUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites)).isTrue();

    val exception = assertThrows(RuntimeException.class,
            ()-> programService.updateProgram(programToUpdate, Collections.EMPTY_LIST, Collections.EMPTY_LIST, institutions, countries, regions));
    assertThat(exception.getMessage()).isEqualTo("INVALID_ARGUMENT: Cannot update program, a program must have at least one of each: cancer, primary site, institution, country, and region.");
  }

  @Test
  public void add_invalid_cancer_valid_primary_site_fail(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    entityGenerator.setUpCancer("Soft Tissue cancer");
    entityGenerator.setUpPrimarySite("Liver");
    entityGenerator.setUpPrimarySite("Brain");
    entityGenerator.setUpPrimarySite("Bone");
    entityGenerator.setUpPrimarySite("Skin");
    entityGenerator.setUpInstitution("OICR");
    entityGenerator.setUpCountry("CA");
    entityGenerator.setUpRegion("NORTH AMERICA");

    val cancers = List.of("Soft Tissue cancer");
    val primarySites = List.of("Liver", "Brain");
    val institutions = List.of("OICR");
    val regions = List.of("NORTH AMERICA");
    val countries = List.of("CA");
    programService.updateProgram(programToUpdate, cancers, primarySites, institutions, countries, regions);

    val cancersBeforeUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySiteBeforeUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersBeforeUpdate.size()).isEqualTo(1);
    assertThat(cancersBeforeUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers)).isTrue();

    assertThat(primarySiteBeforeUpdate.size()).isEqualTo(2);
    assertThat(primarySiteBeforeUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites)).isTrue();

    val cancers2 = List.of("Unicorn cancer");
    val primarySites2 = List.of("Bone", "Skin");
    val exception = assertThrows(NotFoundException.class,
            () -> programService.updateProgram(programToUpdate, cancers2, primarySites2, institutions, countries, regions));
    assertThat(exception.getMessage().contains("Unicorn cancer"));
  }

  @Test
  public void add_valid_cancer_invalid_primary_site_fail(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    entityGenerator.setUpCancer("Soft Tissue cancer");
    entityGenerator.setUpCancer("Skin cancer");
    entityGenerator.setUpCancer("Bone cancer");
    entityGenerator.setUpPrimarySite("Liver");
    entityGenerator.setUpPrimarySite("Brain");
    entityGenerator.setUpInstitution("OICR");
    entityGenerator.setUpCountry("CA");
    entityGenerator.setUpRegion("NORTH AMERICA");

    val cancers = List.of("Soft Tissue cancer", "Skin cancer");
    val primarySites = List.of("Liver", "Brain");
    val institutions = List.of("OICR");
    val regions = List.of("NORTH AMERICA");
    val countries = List.of("CA");

    programService.updateProgram(programToUpdate, cancers, primarySites, institutions, countries, regions);

    val cancersBeforeUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySiteBeforeUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersBeforeUpdate.size()).isEqualTo(2);
    assertThat(cancersBeforeUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers)).isTrue();

    assertThat(primarySiteBeforeUpdate.size()).isEqualTo(2);
    assertThat(primarySiteBeforeUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites)).isTrue();

    val cancers2 = List.of("Bone cancer");
    val primarySites2 = List.of("Unicorn", "Hair");
    val exception = assertThrows(NotFoundException.class,
            () -> programService.updateProgram(programToUpdate, cancers2, primarySites2, institutions, countries, regions));
    assertThat(exception.getMessage().contains("Unicorn"));
    assertThat(exception.getMessage().contains("Hair"));
  }

  @Test
  public void add_invalid_cancer_invalid_primary_site_fail(){
    val programToUpdate = entityGenerator.setUpProgramEntity("PROGRAM-CA");
    val programId = programToUpdate.getId();
    entityGenerator.setUpCancer("Soft Tissue cancer");
    entityGenerator.setUpCancer("Skin cancer");
    entityGenerator.setUpPrimarySite("Liver");
    entityGenerator.setUpPrimarySite("Brain");
    entityGenerator.setUpInstitution("OICR");
    entityGenerator.setUpCountry("CA");
    entityGenerator.setUpRegion("NORTH AMERICA");

    val cancers = List.of("Soft Tissue cancer", "Skin cancer");
    val primarySites = List.of("Liver", "Brain");
    val institutions = List.of("OICR");
    val regions = List.of("NORTH AMERICA");
    val countries = List.of("CA");
    programService.updateProgram(programToUpdate, cancers, primarySites, institutions, countries, regions);

    val cancersBeforeUpdate = cancerRepository.findAll(CancerSpecification.containsProgram(programId));
    val primarySiteBeforeUpdate = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(programId));

    assertThat(cancersBeforeUpdate.size()).isEqualTo(2);
    assertThat(cancersBeforeUpdate.stream().map(CancerEntity::getName).collect(toList()).containsAll(cancers)).isTrue();

    assertThat(primarySiteBeforeUpdate.size()).isEqualTo(2);
    assertThat(primarySiteBeforeUpdate.stream().map(PrimarySiteEntity::getName).collect(toList()).containsAll(primarySites)).isTrue();

    val cancers2 = List.of("Fingernail cancer");
    val primarySites2 = List.of("Unicorn", "Hair");
    val exception = assertThrows(NotFoundException.class,
            () -> programService.updateProgram(programToUpdate, cancers2, primarySites2, institutions, countries, regions));
    assertThat(exception.getMessage().contains("Fingernail cancer"));
  }

}
