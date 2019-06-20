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
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.PrimarySiteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.repositories.*;
import org.icgc.argo.program_service.repositories.query.CancerSpecification;
import org.icgc.argo.program_service.repositories.query.PrimarySiteSpecification;
import org.icgc.argo.program_service.repositories.query.ProgramSpecificationBuilder;
import org.icgc.argo.program_service.utils.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static org.icgc.argo.program_service.model.join.ProgramCancer.createProgramCancer;
import static org.icgc.argo.program_service.model.join.ProgramPrimarySite.createProgramPrimarySite;
import static org.icgc.argo.program_service.utils.CollectionUtils.*;
import static org.icgc.argo.program_service.utils.EntityService.getManyEntities;

@Service
@Validated
@Slf4j
public class ProgramService {

  /**
   * Dependencies
   */
  private final ProgramRepository programRepository;
  private final CancerRepository cancerRepository;
  private final PrimarySiteRepository primarySiteRepository;
  private final ProgramConverter programConverter;
  private final ProgramCancerRepository programCancerRepository;
  private final ProgramPrimarySiteRepository programPrimarySiteRepository;
  @Autowired
  public ProgramService(
    @NonNull ProgramRepository programRepository,
    @NonNull CancerRepository cancerRepository,
    @NonNull PrimarySiteRepository primarySiteRepository,
    @NonNull ProgramConverter programConverter,
    @NonNull ProgramCancerRepository programCancerRepository,
    @NonNull ProgramPrimarySiteRepository programPrimarySiteRepository
  ) {
    this.programRepository = programRepository;
    this.cancerRepository = cancerRepository;
    this.primarySiteRepository = primarySiteRepository;
    this.programConverter = programConverter;
    this.programCancerRepository = programCancerRepository;
    this.programPrimarySiteRepository = programPrimarySiteRepository;
  }

  public ProgramEntity getProgram(@NonNull String name) {
    val search = programRepository.findByShortName(name);
    if (search.isEmpty()) {
      throw Status.NOT_FOUND.
        withDescription("Program '" + name + "' not found").
        asRuntimeException();
    }
    val program = search.get();
    val uuid = program.getId();
    val primarySites = primarySiteRepository.findAll(PrimarySiteSpecification.containsProgram(uuid));
    val cancers = cancerRepository.findAll(CancerSpecification.containsProgram(uuid));

    List<ProgramCancer> programCancers = mapToList(cancers, x -> createProgramCancer(program, x).get());
    List<ProgramPrimarySite> programPrimarySites = mapToList(primarySites, x -> createProgramPrimarySite(program, x).get());
    program.setProgramCancers(new TreeSet<>(programCancers));
    program.setProgramPrimarySites(new TreeSet<>(programPrimarySites));

    return program;
  }

  //TODO: add existence check, and ensure program doesnt already exist. If it does, return a Conflict
  public ProgramEntity createProgram(@NonNull Program program)
    throws DataIntegrityViolationException {
    val programEntity = programConverter.programToProgramEntity(program);
    // Set the timestamps
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    programEntity.setCreatedAt(now);
    programEntity.setUpdatedAt(now);

    val p = programRepository.save(programEntity);

    val cancers = cancerRepository.findAllByNameIn(program.getCancerTypesList());
    val primarySites = primarySiteRepository.findAllByNameIn(program.getPrimarySitesList());

    List<ProgramCancer> programCancers = mapToList(cancers, x -> createProgramCancer(p, x).get());
    List<ProgramPrimarySite> programPrimarySites = mapToList(primarySites, x -> createProgramPrimarySite(p, x).get());

    programCancerRepository.saveAll(programCancers);
    programPrimarySiteRepository.saveAll(programPrimarySites);

    return programEntity;
  }

  private List<PrimarySiteEntity> getPrimarySites(List<String> primarySitesList) {
    return primarySitesList.stream().
      map(this::getPrimarySite).
      collect(Collectors.toList());
  }

  public ProgramEntity updateProgram(@NonNull ProgramEntity updatingProgram)
    throws NotFoundException, EmptyResultDataAccessException {
    val programToUpdate = getProgram(updatingProgram.getShortName());
    val id = programToUpdate.getId();
    //update associations
    processCancers(programToUpdate, updatingProgram);
    processPrimarySites(programToUpdate, updatingProgram);

    // update basic info program
    programConverter.updateProgram(updatingProgram, programToUpdate);

    programRepository.save(programToUpdate);
    return programToUpdate;
  }

  private CancerEntity getCancer(String cancerType) {
    val result = cancerRepository.getCancerByName(cancerType);
    if (result.isEmpty()) {
      throw Status.INVALID_ARGUMENT.augmentDescription("Invalid cancer type '" + cancerType + "'").asRuntimeException();
    }
    return result.get();
  }

  private PrimarySiteEntity getPrimarySite(String primarySite) {
    val result = primarySiteRepository.getPrimarySiteByName(primarySite);
    if (result.isEmpty()) {
      throw Status.INVALID_ARGUMENT.augmentDescription("Invalid primary site" + primarySite).asRuntimeException();
    }
    return result.get();
  }

  private void processCancers(@NonNull ProgramEntity programToUpdate, @NonNull ProgramEntity updatingProgram)
    throws EmptyResultDataAccessException {
    if (!nullOrEmpty(updatingProgram.getProgramCancers())) {
      val updatingCancers = mapToImmutableSet(updatingProgram.getProgramCancers(), ProgramCancer::getCancer);
      val updatingCancerIds = convertToIds(updatingCancers);
      getManyEntities(CancerEntity.class, cancerRepository, updatingCancerIds);
      programToUpdate.setProgramCancers(updatingProgram.getProgramCancers());
    } else {
      programToUpdate.setProgramCancers(Collections.emptySet());
    }
  }

  private void processPrimarySites(@NonNull ProgramEntity programToUpdate, @NonNull ProgramEntity updatingProgram)
    throws EmptyResultDataAccessException {

    if (!nullOrEmpty(updatingProgram.getProgramPrimarySites())) {
      val updatingPrimarySites = mapToImmutableSet(updatingProgram.getProgramPrimarySites(),
        ProgramPrimarySite::getPrimarySite);
      val updatingPrimarySiteIds = convertToIds(updatingPrimarySites);
      getManyEntities(PrimarySiteEntity.class, primarySiteRepository, updatingPrimarySiteIds);
      programToUpdate.setProgramPrimarySites(updatingProgram.getProgramPrimarySites());
    } else {
      programToUpdate.setProgramPrimarySites(Collections.emptySet());
    }
  }

  public void removeProgram(String name) throws EmptyResultDataAccessException {
    val p = getProgram(name);
    programRepository.deleteById(p.getId());
  }

  public List<ProgramEntity> listPrograms() {
    val programs= programRepository.findAll(new ProgramSpecificationBuilder()
      .setFetchCancers(true)
      .setFetchPrimarySites(true)
      .listAll());
    return List.copyOf(new LinkedHashSet<ProgramEntity>(programs));
  }

}