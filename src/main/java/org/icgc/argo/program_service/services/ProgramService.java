/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.icgc.argo.program_service.services;

import io.grpc.Status;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.*;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.model.join.*;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.repositories.*;
import org.icgc.argo.program_service.repositories.query.ProgramSpecificationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.icgc.argo.program_service.model.join.ProgramCancer.createProgramCancer;
import static org.icgc.argo.program_service.model.join.ProgramCountry.createProgramCountry;
import static org.icgc.argo.program_service.model.join.ProgramInstitution.createProgramInstitution;
import static org.icgc.argo.program_service.model.join.ProgramPrimarySite.createProgramPrimarySite;
import static org.icgc.argo.program_service.model.join.ProgramRegion.createProgramRegion;
import static org.icgc.argo.program_service.utils.CollectionUtils.findDuplicates;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToList;
import static org.icgc.argo.program_service.utils.EntityService.checkExistenceByName;
import static org.icgc.argo.program_service.utils.EntityService.*;

@Service
@Validated
@Slf4j
public class ProgramService {

  /** Dependencies */
  private final ProgramRepository programRepository;
  private final CancerRepository cancerRepository;
  private final PrimarySiteRepository primarySiteRepository;
  private final InstitutionRepository institutionRepository;
  private final RegionRepository regionRepository;
  private final CountryRepository countryRepository;
  private final ProgramConverter programConverter;
  private final ProgramCancerRepository programCancerRepository;
  private final ProgramPrimarySiteRepository programPrimarySiteRepository;
  private final ProgramInstitutionRepository programInstitutionRepository;
  private final ProgramRegionRepository programRegionRepository;
  private final ProgramCountryRepository programCountryRepository;

  @Autowired
  public ProgramService(
      @NonNull ProgramRepository programRepository,
      @NonNull CancerRepository cancerRepository,
      @NonNull PrimarySiteRepository primarySiteRepository,
      @NonNull ProgramConverter programConverter,
      @NonNull ProgramCancerRepository programCancerRepository,
      @NonNull ProgramPrimarySiteRepository programPrimarySiteRepository,
      @NonNull InstitutionRepository institutionRepository,
      @NonNull RegionRepository regionRepository,
      @NonNull CountryRepository countryRepository,
      @NonNull ProgramInstitutionRepository programInstitutionRepository,
      @NonNull ProgramRegionRepository programRegionRepository,
      @NonNull ProgramCountryRepository programCountryRepository) {
    this.programRepository = programRepository;
    this.cancerRepository = cancerRepository;
    this.primarySiteRepository = primarySiteRepository;
    this.programConverter = programConverter;
    this.programCancerRepository = programCancerRepository;
    this.programPrimarySiteRepository = programPrimarySiteRepository;
    this.institutionRepository = institutionRepository;
    this.regionRepository = regionRepository;
    this.countryRepository = countryRepository;
    this.programInstitutionRepository = programInstitutionRepository;
    this.programRegionRepository = programRegionRepository;
    this.programCountryRepository = programCountryRepository;
  }

  private ProgramEntity findProgramByShortName(@NonNull String name) {
    val search = programRepository.findByShortName(name);
    if (search.isEmpty()) {
      throw Status.NOT_FOUND
          .withDescription("Program '" + name + "' not found")
          .asRuntimeException();
    }
    return search.get();
  }

  public ProgramEntity getProgram(@NonNull String name) {
    return findProgramByShortName(name);
  }

  @Transactional
  public ProgramEntity createWithSideEffectTransactional(@NonNull Program program, Consumer<ProgramEntity> consumer) {
    val programEntity = createProgram(program);
    consumer.accept(programEntity);
    return programEntity;
  }

  public ProgramEntity createProgram(@NonNull Program program)
      throws DataIntegrityViolationException {
    val shortName = CommonConverter.INSTANCE.unboxStringValue(program.getShortName());
    if(programRepository.findByShortName(shortName).isPresent()) {
            throw Status.INVALID_ARGUMENT
                    .augmentDescription(format("Program %s already exists.", shortName))
                    .asRuntimeException();
    }

    val programEntity = programConverter.programToProgramEntity(program);
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    if (program.getCancerTypesList().isEmpty()
        || program.getPrimarySitesList().isEmpty()
        || program.getInstitutionsList().isEmpty()
        || program.getRegionsList().isEmpty()
        || program.getCountriesList().isEmpty()) {
      throw Status.INVALID_ARGUMENT
          .augmentDescription(
            "Cannot create program. Must provide at least one of each: cancer, primary site, institution, country, and region.")
          .asRuntimeException();
    }

    val cancers = cancerRepository.findAllByNameIn(program.getCancerTypesList());
    val primarySites = primarySiteRepository.findAllByNameIn(program.getPrimarySitesList());
    val regions = regionRepository.findAllByNameIn(program.getRegionsList());
    val countries = countryRepository.findAllByNameIn(program.getCountriesList());

    // Verify inputs, throwing exceptions on bad inputs
    compareLists("Cannot create program, invalid cancers provided:%s", cancers, program.getCancerTypesList());
    compareLists("Cannot create program, invalid primary sites provided:%s", primarySites, program.getPrimarySitesList());
    compareLists("Cannot create program, invalid countries provided:%s", countries, program.getCountriesList());
    compareLists("Cannot create program, invalid regions provided:%s", regions, program.getRegionsList());

    // Add new institutions if we must
    List<InstitutionEntity> institutions = institutionRepository.findAllByNameIn(program.getInstitutionsList()); // MUTABLE
    if (institutions.size() != program.getInstitutionsList().size()) {
      institutions = filterAndAddInstitutions(program.getInstitutionsList());
    }

    val p = programRepository.save(programEntity);

    List<ProgramCancer> programCancers = mapToList(cancers, x -> createProgramCancer(p, x).get());
    List<ProgramPrimarySite> programPrimarySites =
        mapToList(primarySites, x -> createProgramPrimarySite(p, x).get());
    List<ProgramInstitution> programInstitutions =
        mapToList(institutions, x -> createProgramInstitution(p, x).get());
    List<ProgramRegion> programRegions = mapToList(regions, x -> createProgramRegion(p, x).get());
    List<ProgramCountry> programCountries =
        mapToList(countries, x -> createProgramCountry(p, x).get());

    programCancerRepository.saveAll(programCancers);
    programPrimarySiteRepository.saveAll(programPrimarySites);
    programInstitutionRepository.saveAll(programInstitutions);
    programRegionRepository.saveAll(programRegions);
    programCountryRepository.saveAll(programCountries);

    return programEntity;
  }

  private List<InstitutionEntity> filterAndAddInstitutions(@NonNull List<String> names){
    val duplicates = findDuplicates(names);
    if(!duplicates.isEmpty()){
      throw Status.INVALID_ARGUMENT
              .augmentDescription(format("Please remove duplicate institutions %s in the list.", duplicates))
              .asRuntimeException();
    }
    List<InstitutionEntity> institutions = institutionRepository.findAllByNameIn(names);
    val existing = institutions.stream().map(InstitutionEntity::getName).collect(toUnmodifiableSet());
    names.stream().filter(i -> !existing.contains(i)).forEach( i -> {
              val newInstitute = new InstitutionEntity();
              newInstitute.setName(i);
              institutionRepository.save(newInstitute);
            }
    );
    institutions = institutionRepository.findAllByNameIn(names);
    if (institutions.size() != names.size()) {
      throw new IllegalStateException("Was unable to add new institutions"); // Final Sanity Check
    }
    return institutions;
  }

  @Transactional
  public ProgramEntity updateProgram(
      @NonNull ProgramEntity updatingProgram,
      @NonNull List<String> cancers,
      @NonNull List<String> primarySites,
      @NonNull List<String> institutions,
      @NonNull List<String> countries,
      @NonNull List<String> regions)
      throws NotFoundException {
    if (cancers.isEmpty()
        || primarySites.isEmpty()
        || institutions.isEmpty()
        || countries.isEmpty()
        || regions.isEmpty()) {
      throw Status.INVALID_ARGUMENT
          .augmentDescription(
              "Cannot update program. Cancer, primary site, institution, country, and region cannot be empty.")
          .asRuntimeException();
    }

    val programToUpdate = findProgramByShortName(updatingProgram.getShortName());

    // update associations
    processCancers(programToUpdate, cancers);
    processPrimarySites(programToUpdate, primarySites);
    processInstitutions(programToUpdate, institutions);
    processCountries(programToUpdate, countries);
    processRegions(programToUpdate, regions);

    // update program info
    programConverter.updateProgram(updatingProgram, programToUpdate);
    programRepository.save(programToUpdate);
    return programToUpdate;
  }

  private void processCancers(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> cancerNames) {
    val cancerEntities = checkExistenceByName(CancerEntity.class, cancerRepository, cancerNames);

    programCancerRepository.deleteAllByProgramId(programToUpdate.getId());
    val programCancers =
        cancerEntities.stream()
            .map(c -> createProgramCancer(programToUpdate, c))
            .map(Optional::get)
            .collect(toUnmodifiableList());
    programCancerRepository.saveAll(programCancers);
  }

  private void processPrimarySites(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> primarySitesNames) {
    val primarySiteEntities =
        checkExistenceByName(PrimarySiteEntity.class, primarySiteRepository, primarySitesNames);

    programPrimarySiteRepository.deleteAllByProgramId(programToUpdate.getId());
    val programPrimarySites =
        primarySiteEntities.stream()
            .map(ps -> createProgramPrimarySite(programToUpdate, ps))
            .map(Optional::get)
            .collect(toUnmodifiableList());
    programPrimarySiteRepository.saveAll(programPrimarySites);
  }

  private void processInstitutions(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> names) {
    val existing = institutionRepository.findAllByNameIn(names);
    // Add new institutions
    val entities = names.size() != existing.size() ? filterAndAddInstitutions(names) : existing;
    programInstitutionRepository.deleteAllByProgramId(programToUpdate.getId());
    val programInstitutions =
        entities.stream()
            .map(c -> createProgramInstitution(programToUpdate, c))
            .map(Optional::get)
            .collect(toUnmodifiableList());
    programInstitutionRepository.saveAll(programInstitutions);
  }

  private void processCountries(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> names) {
    val countryEntities = checkExistenceByName(CountryEntity.class, countryRepository, names);

    programCountryRepository.deleteAllByProgramId(programToUpdate.getId());
    val programCountries =
        countryEntities.stream()
            .map(c -> createProgramCountry(programToUpdate, c))
            .map(Optional::get)
            .collect(toUnmodifiableList());
    programCountryRepository.saveAll(programCountries);
  }

  private void processRegions(@NonNull ProgramEntity programToUpdate, @NonNull List<String> names) {
    val regionEntities = checkExistenceByName(RegionEntity.class, regionRepository, names);

    programRegionRepository.deleteAllByProgramId(programToUpdate.getId());
    val programRegions =
        regionEntities.stream()
            .map(c -> createProgramRegion(programToUpdate, c))
            .map(Optional::get)
            .collect(toUnmodifiableList());
    programRegionRepository.saveAll(programRegions);
  }

  /**
   * Compares the user provided list against the list stored in the system.
   * Throws an exception if it determines that the users provided a collection that cannot
   * be found by the system.
   *
   * Precondition: System List should always be a subset of User List
   *
   * @param errorTemplate String template for the exception message.
   * @param userList The user provided collection.
   * @param systemList The system collection.
   */
  @SuppressWarnings("unchecked")
  static void compareLists(String errorTemplate, List userList, List systemList) {
    if (userList.size() > systemList.size()) { // Couldn't find all of them
      val badInput = new StringBuilder();
      userList.forEach(item -> {
        if (!systemList.contains(item)) {
          badInput.append(" ");
          badInput.append(item.toString());
        }
      });
      val message = String.format(errorTemplate, badInput.toString());
      throw Status.INVALID_ARGUMENT.augmentDescription(message).asRuntimeException();
    }
  }

  public void removeProgram(String name) throws EmptyResultDataAccessException {
    val p = getProgram(name);
    programRepository.deleteById(p.getId());
    log.info("Program {} is successfully deleted. ", name);
  }

  public List<ProgramEntity> listPrograms() {
    val programs =
        programRepository.findAll(
            new ProgramSpecificationBuilder()
                .setFetchCancers(true)
                .setFetchPrimarySites(true)
                .setFetchInstitutions(true)
                .setFetchCountries(true)
                .setFetchRegions(true)
                .listAll());
    return List.copyOf(programs);
  }

  public List<CancerEntity> listCancers() {
    return List.copyOf(cancerRepository.findAll());
  }

  public List<PrimarySiteEntity> listPrimarySites() {
    return List.copyOf(primarySiteRepository.findAll());
  }

  public List<CountryEntity> listCountries(){
    return List.copyOf(countryRepository.findAll());
  }

  public List<RegionEntity> listRegions(){
    return List.copyOf(regionRepository.findAll());
  }

  public List<InstitutionEntity> listInstitutions() {
    return List.copyOf(institutionRepository.findAll());
  }

  public List<InstitutionEntity> addInstitutions(@NonNull List<String> names){
    checkEmpty(names);
    checkDuplicate(InstitutionEntity.class, institutionRepository, names);
    val entities = names.stream().map(name -> new InstitutionEntity().setName(name)).collect(toUnmodifiableList());
    return institutionRepository.saveAll(entities);
  }

}
