package org.icgc.argo.program_service.utils;

import lombok.val;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.MembershipType;
import org.icgc.argo.program_service.repositories.CancerRepository;
import org.icgc.argo.program_service.repositories.PrimarySiteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class EntityGenerator {

  @Autowired
  private ProgramRepository programRepository;

  @Autowired
  private CancerRepository cancerRepository;

  @Autowired
  private PrimarySiteRepository primarySiteRepository;

  public ProgramEntity setUpProgramEntity(String shortname) {
    return programRepository
            .findByShortName(shortname)
            .orElseGet(
                    () -> {
                      return createProgramEntity(shortname);
                    });
  }

  public ProgramEntity createProgramEntity(String shortname){
    val entity = new ProgramEntity().
            setShortName(shortname).
            setCommitmentDonors(1000).
            setCountries("Canada").
            setCreatedAt(LocalDateTime.now()).
            setDescription("Test Program").
            setGenomicDonors(1000).
            setId(UUID.randomUUID()).
            setInstitutions("Institute of Institutions").
            setMembershipType(MembershipType.ASSOCIATE).
            setName("Program One").
            setSubmittedDonors(1000).
            setUpdatedAt(LocalDateTime.now()).
            setWebsite("http://test.org");
    return programRepository.save(entity);
  }

  public CancerEntity setUpCancer(String name){
    return cancerRepository.getCancerByName(name)
            .orElseGet(
                    () -> { return createCancerEntity(name); });
  }

  public PrimarySiteEntity setUpPrimarySite(String name){
    return primarySiteRepository.getPrimarySiteByName(name)
            .orElseGet(
                    () -> { return createPrimarySite(name); });
  }

  private CancerEntity createCancerEntity(String name) {
    val entity = new CancerEntity().setId(UUID.randomUUID()).setName(name);
    return cancerRepository.save(entity);
  }

  private PrimarySiteEntity createPrimarySite(String name) {
    val entity = new PrimarySiteEntity().setId(UUID.randomUUID()).setName(name);
    return primarySiteRepository.save(entity);
  }

}
