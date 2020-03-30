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

import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static org.icgc.argo.program_service.model.join.ProgramCancer.createProgramCancer;
import static org.icgc.argo.program_service.model.join.ProgramCountry.createProgramCountry;
import static org.icgc.argo.program_service.model.join.ProgramInstitution.createProgramInstitution;
import static org.icgc.argo.program_service.model.join.ProgramPrimarySite.createProgramPrimarySite;
import static org.icgc.argo.program_service.model.join.ProgramRegion.createProgramRegion;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;
import static org.icgc.argo.program_service.utils.CollectionUtils.mapToSet;
import static org.icgc.argo.program_service.utils.EntityService.*;

import io.grpc.Status;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.validation.ValidatorFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
import org.springframework.validation.annotation.Validated;

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
  private final ValidatorFactory validatorFactory;

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
      @NonNull ProgramCountryRepository programCountryRepository,
      @NonNull ValidatorFactory validatorFactory) {
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
    this.validatorFactory = validatorFactory;
  }

  private ProgramEntity findProgramByShortName(@NonNull String name, boolean allowInactive) {
    val search =
        programRepository.findOne(
            new ProgramSpecificationBuilder()
                .setFetchCancers(true)
                .setFetchPrimarySites(true)
                .setFetchInstitutions(true)
                .setFetchCountries(true)
                .setFetchRegions(true)
                .buildByShortName(name));

    if (search.isEmpty()) {
      throw Status.NOT_FOUND
          .withDescription("Program '" + name + "' not found")
          .asRuntimeException();
    } else if (!(allowInactive || search.get().getActive().booleanValue())) {
      // Not allowing inactive programs, and the program is inactive
      throw Status.FAILED_PRECONDITION
          .withDescription("Program '" + name + "' is inactive.")
          .asRuntimeException();
    }
    return search.get();
  }

  public ProgramEntity getProgram(@NonNull String name) {
    return getProgram(name, false);
  }

  public ProgramEntity getProgram(@NonNull String name, boolean allowInactive) {
    return findProgramByShortName(name, allowInactive);
  }

  public ProgramEntity createWithSideEffect(
      @NonNull Program program, Consumer<ProgramEntity> consumer) {
    val programEntity = createProgram(program);
    consumer.accept(programEntity);
    return programEntity;
  }

  public ProgramEntity createProgram(@NonNull Program program)
      throws DataIntegrityViolationException {
    val programEntity = programConverter.programToProgramEntity(program);

    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    val p = programRepository.save(programEntity);
    val cancers = cancerRepository.findAllByNameIn(program.getCancerTypesList());
    val primarySites = primarySiteRepository.findAllByNameIn(program.getPrimarySitesList());
    val regions = regionRepository.findAllByNameIn(program.getRegionsList());
    val countries = countryRepository.findAllByNameIn(program.getCountriesList());

    // Add new institutions if we must
    List<InstitutionEntity> institutions =
        institutionRepository.findAllByNameIn(program.getInstitutionsList()); // MUTABLE
    if (institutions.size() != program.getInstitutionsList().size()) {
      institutions = filterAndAddInstitutions(program.getInstitutionsList());
    }

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

  private List<InstitutionEntity> filterAndAddInstitutions(@NonNull List<String> names) {
    val duplicates = findDuplicates(names);
    if (!duplicates.isEmpty()) {
      throw Status.INVALID_ARGUMENT
          .augmentDescription(
              format("Please remove duplicate institutions %s in the list.", duplicates))
          .asRuntimeException();
    }
    List<InstitutionEntity> institutions = institutionRepository.findAllByNameIn(names);
    val existing =
        institutions.stream().map(InstitutionEntity::getName).collect(toUnmodifiableSet());
    names.stream()
        .filter(i -> !existing.contains(i))
        .forEach(
            i -> {
              val newInstitute = new InstitutionEntity();
              newInstitute.setName(i);
              institutionRepository.save(newInstitute);
            });
    institutions = institutionRepository.findAllByNameIn(names);
    if (institutions.size() != names.size()) {
      throw new IllegalStateException("Was unable to add new institutions"); // Final Sanity Check
    }
    return institutions;
  }

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

    val programToUpdate = findProgramByShortName(updatingProgram.getShortName(), false);

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

  public ProgramEntity activateProgram(
      @NonNull ProgramEntity program, @NonNull String updatedShortName) throws NotFoundException {

    if (program.getActive().booleanValue()) {
      throw Status.FAILED_PRECONDITION
          .withDescription("The program '" + program.getShortName() + "' is already active.")
          .asRuntimeException();
    }
    // update program info
    program.setShortName(updatedShortName);
    program.setActive(true);
    programRepository.save(program);
    return program;
  }

  private void processCancers(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> cancerNames) {
    val cancerEntities = checkExistenceByName(CancerEntity.class, cancerRepository, cancerNames);
    val currentCancers = mapToSet(programToUpdate.getProgramCancers(), ProgramCancer::getCancer);

    val toDelete =
        currentCancers.stream().filter(c -> !cancerEntities.contains(c)).collect(toSet());
    val toAdd =
        cancerEntities.stream()
            .filter(c -> !currentCancers.contains(c))
            .map(c -> createProgramCancer(programToUpdate, c))
            .map(Optional::get)
            .collect(toSet());

    programToUpdate.getProgramCancers().removeIf(programCancerPredicate(programToUpdate, toDelete));
    currentCancers.forEach(
        c -> c.getProgramCancers().removeIf(programCancerPredicate(programToUpdate, toDelete)));
    programCancerRepository.saveAll(toAdd);
  }

  private void processPrimarySites(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> primarySitesNames) {
    val primarySiteEntities =
        checkExistenceByName(PrimarySiteEntity.class, primarySiteRepository, primarySitesNames);
    val currentPrimarySites =
        mapToSet(programToUpdate.getProgramPrimarySites(), ProgramPrimarySite::getPrimarySite);

    val toDelete =
        currentPrimarySites.stream().filter(p -> !primarySiteEntities.contains(p)).collect(toSet());
    val toAdd =
        primarySiteEntities.stream()
            .filter(p -> !currentPrimarySites.contains(p))
            .map(p -> createProgramPrimarySite(programToUpdate, p))
            .map(Optional::get)
            .collect(toSet());

    programToUpdate
        .getProgramPrimarySites()
        .removeIf(programPrimarySitePredicate(programToUpdate, toDelete));
    currentPrimarySites.forEach(
        p ->
            p.getProgramPrimarySites()
                .removeIf(programPrimarySitePredicate(programToUpdate, toDelete)));
    programPrimarySiteRepository.saveAll(toAdd);
  }

  private void processInstitutions(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> institutionNames) {
    // convert institution names to institution entities and add missing institutions to repo
    val updatedInstitutionEntities = filterAndAddInstitutions(institutionNames);
    val currentInstitutionEntities =
        mapToSet(programToUpdate.getProgramInstitutions(), ProgramInstitution::getInstitution);

    val institutionsToRemove =
        currentInstitutionEntities.stream()
            .filter(i -> !updatedInstitutionEntities.contains(i))
            .collect(toSet());
    val programInstitutionsToAdd =
        updatedInstitutionEntities.stream()
            .filter(i -> !currentInstitutionEntities.contains(i))
            .map(i -> createProgramInstitution(programToUpdate, i))
            .map(Optional::get)
            .collect(toSet());

    programToUpdate
        .getProgramInstitutions()
        .removeIf(programInstitutionPredicate(programToUpdate, institutionsToRemove));
    currentInstitutionEntities.forEach(
        p ->
            p.getProgramInstitutions()
                .removeIf(programInstitutionPredicate(programToUpdate, institutionsToRemove)));

    programInstitutionRepository.saveAll(programInstitutionsToAdd);
  }

  private void processCountries(
      @NonNull ProgramEntity programToUpdate, @NonNull List<String> names) {
    val countryEntities = checkExistenceByName(CountryEntity.class, countryRepository, names);
    val currentCountries =
        mapToSet(programToUpdate.getProgramCountries(), ProgramCountry::getCountry);

    val toDelete =
        currentCountries.stream().filter(c -> !countryEntities.contains(c)).collect(toSet());
    val toAdd =
        countryEntities.stream()
            .filter(c -> !currentCountries.contains(c))
            .map(c -> createProgramCountry(programToUpdate, c))
            .map(Optional::get)
            .collect(toSet());

    programToUpdate
        .getProgramCountries()
        .removeIf(programCountryPredicate(programToUpdate, toDelete));
    currentCountries.forEach(
        c -> c.getProgramCountries().removeIf(programCountryPredicate(programToUpdate, toDelete)));
    programCountryRepository.saveAll(toAdd);
  }

  private void processRegions(@NonNull ProgramEntity programToUpdate, @NonNull List<String> names) {
    val regionEntities = checkExistenceByName(RegionEntity.class, regionRepository, names);
    val currentRegions = mapToSet(programToUpdate.getProgramRegions(), ProgramRegion::getRegion);

    val toDelete =
        currentRegions.stream().filter(r -> !regionEntities.contains(r)).collect(toSet());
    val toAdd =
        regionEntities.stream()
            .filter(r -> !currentRegions.contains(r))
            .map(r -> createProgramRegion(programToUpdate, r))
            .map(Optional::get)
            .collect(toSet());

    programToUpdate.getProgramRegions().removeIf(programRegionPredicate(programToUpdate, toDelete));
    currentRegions.forEach(
        r -> r.getProgramRegions().removeIf(programRegionPredicate(programToUpdate, toDelete)));
    programRegionRepository.saveAll(toAdd);
  }

  /**
   * Compares the user provided list against the list stored in the system. Throws an exception if
   * it determines that the users provided a collection that cannot be found by the system.
   *
   * <p>Precondition: System List should always be a subset of User List
   *
   * @param errorTemplate String template for the exception message.
   * @param userList The user provided collection.
   * @param systemList The system collection.
   */
  @SuppressWarnings("unchecked")
  static void compareLists(String errorTemplate, List userList, List systemList) {
    if (userList.size() > systemList.size()) { // Couldn't find all of them
      val badInput = new StringBuilder();
      userList.forEach(
          item -> {
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
                .listAll(true));
    return List.copyOf(programs);
  }

  List<CancerEntity> listCancers() {
    return List.copyOf(cancerRepository.findAll());
  }

  List<PrimarySiteEntity> listPrimarySites() {
    return List.copyOf(primarySiteRepository.findAll());
  }

  List<CountryEntity> listCountries() {
    return List.copyOf(countryRepository.findAll());
  }

  List<RegionEntity> listRegions() {
    return List.copyOf(regionRepository.findAll());
  }

  List<InstitutionEntity> listInstitutions() {
    return List.copyOf(institutionRepository.findAll());
  }

  List<InstitutionEntity> addInstitutions(@NonNull List<String> names) {
    checkEmpty(names);
    checkDuplicate(InstitutionEntity.class, institutionRepository, names);
    val entities =
        names.stream()
            .map(name -> new InstitutionEntity().setName(name))
            .collect(toUnmodifiableList());
    return institutionRepository.saveAll(entities);
  }

  private static Predicate<ProgramCancer> programCancerPredicate(
      ProgramEntity program, Set<CancerEntity> cancers) {
    val id = program.getId();
    return c -> c.getProgram().getId().equals(id) && cancers.contains(c.getCancer());
  }

  private static Predicate<ProgramPrimarySite> programPrimarySitePredicate(
      ProgramEntity program, Set<PrimarySiteEntity> sites) {
    val id = program.getId();
    return ps -> ps.getProgram().getId().equals(id) && sites.contains(ps.getPrimarySite());
  }

  private static Predicate<ProgramInstitution> programInstitutionPredicate(
      ProgramEntity program, Set<InstitutionEntity> institutions) {
    val id = program.getId();
    return i -> i.getProgram().getId().equals(id) && institutions.contains(i.getInstitution());
  }

  private static Predicate<ProgramCountry> programCountryPredicate(
      ProgramEntity program, Set<CountryEntity> countries) {
    val id = program.getId();
    return c -> c.getProgram().getId().equals(id) && countries.contains(c.getCountry());
  }

  private static Predicate<ProgramRegion> programRegionPredicate(
      ProgramEntity program, Set<RegionEntity> regions) {
    val id = program.getId();
    return r -> r.getProgram().getId().equals(id) && regions.contains(r.getRegion());
  }
}
