package org.icgc.argo.program_service.utils;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.val;
import org.icgc.argo.program_service.model.entity.*;
import org.icgc.argo.program_service.proto.MembershipType;
import org.icgc.argo.program_service.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EntityGenerator {

  @Autowired private ProgramRepository programRepository;

  @Autowired private CancerRepository cancerRepository;

  @Autowired private PrimarySiteRepository primarySiteRepository;

  @Autowired private InstitutionRepository institutionRepository;

  @Autowired private RegionRepository regionRepository;

  @Autowired private CountryRepository countryRepository;

  public ProgramEntity setUpProgramEntity(String shortname) {
    return programRepository
        .findByShortName(shortname)
        .orElseGet(
            () -> {
              return createProgramEntity(shortname);
            });
  }

  public ProgramEntity createProgramEntity(String shortname) {
    val entity =
        new ProgramEntity()
            .setShortName(shortname)
            .setCommitmentDonors(1000)
            .setCreatedAt(LocalDateTime.now())
            .setDescription("Test Program")
            .setGenomicDonors(1000)
            .setId(UUID.randomUUID())
            .setMembershipType(MembershipType.ASSOCIATE)
            .setName("NAME-" + shortname)
            .setSubmittedDonors(1000)
            .setUpdatedAt(LocalDateTime.now())
            .setWebsite("http://test.org");
    return programRepository.save(entity);
  }

  public CancerEntity setUpCancer(String name) {
    return cancerRepository
        .getCancerByName(name)
        .orElseGet(
            () -> {
              return createCancerEntity(name);
            });
  }

  public PrimarySiteEntity setUpPrimarySite(String name) {
    return primarySiteRepository
        .getPrimarySiteByName(name)
        .orElseGet(
            () -> {
              return createPrimarySite(name);
            });
  }

  public InstitutionEntity setUpInstitution(String name) {
    return institutionRepository
        .getInstitutionByName(name)
        .orElseGet(
            () -> {
              return createInstitutionEntity(name);
            });
  }

  public CountryEntity setUpCountry(String name) {
    return countryRepository
        .getCountryByName(name)
        .orElseGet(
            () -> {
              return createCountryEntity(name);
            });
  }

  public RegionEntity setUpRegion(String name) {
    return regionRepository
        .getRegionByName(name)
        .orElseGet(
            () -> {
              return createRegionEntity(name);
            });
  }

  private CancerEntity createCancerEntity(String name) {
    val entity = new CancerEntity().setId(UUID.randomUUID()).setName(name);
    return cancerRepository.save(entity);
  }

  private PrimarySiteEntity createPrimarySite(String name) {
    val entity = new PrimarySiteEntity().setId(UUID.randomUUID()).setName(name);
    return primarySiteRepository.save(entity);
  }

  private InstitutionEntity createInstitutionEntity(String name) {
    val entity = new InstitutionEntity().setId(UUID.randomUUID()).setName(name);
    return institutionRepository.save(entity);
  }

  private CountryEntity createCountryEntity(String name) {
    val entity = new CountryEntity().setId(UUID.randomUUID()).setName(name);
    return countryRepository.save(entity);
  }

  private RegionEntity createRegionEntity(String name) {
    val entity = new RegionEntity().setId(UUID.randomUUID()).setName(name);
    return regionRepository.save(entity);
  }
}
