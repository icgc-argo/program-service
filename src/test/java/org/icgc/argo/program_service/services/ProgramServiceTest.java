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

import lombok.val;
import net.bytebuddy.utility.RandomString;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ConverterConfig;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.converter.ProgramConverterImpl;
import org.icgc.argo.program_service.model.entity.*;
import org.icgc.argo.program_service.model.join.ProgramCancer;
import org.icgc.argo.program_service.model.join.ProgramCancerId;
import org.icgc.argo.program_service.model.join.ProgramPrimarySite;
import org.icgc.argo.program_service.proto.MembershipType;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.CancerRepository;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.repositories.PrimarySiteRepository;
import org.icgc.argo.program_service.repositories.ProgramRepository;

import org.junit.Before;

import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.MailSender;

import org.springframework.test.util.ReflectionTestUtils;



import java.io.IOException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {
  @InjectMocks
  private ProgramService programService;

  @Mock
  private Program program;

  @Mock
  private ProgramEntity programEntity;

  @Mock
  private JoinProgramInviteRepository invitationRepository;

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private CancerRepository cancerRepository;

  @Mock
  private PrimarySiteRepository primarySiteRepository;

  @Mock
  private MailSender mailSender;

  @Mock
  private JoinProgramInvite invitation;

  @Mock
  private ProgramConverter programConverter;

  @Mock
  private EgoService egoService;

  @Mock
  private MailService mailService;

  @Mock
  private CommonConverter commonConverter;

  void setup() {
    program = Program.newBuilder().
      addAllCancerTypes(List.of("Blood cancer", "Brain cancer")).
      addAllPrimarySites(List.of("Blood", "Brain")).
      build();

    setupCancerRepository();
    setupPrimarySiteRepository();

  }

  void setupCancerRepository() {
    for (val name: List.of("Blood cancer", "Brain cancer")) {
      val entity = createCancerEntity(name);
      when(cancerRepository.getCancerByName(name)).thenReturn(entity);
    }

    ReflectionTestUtils.setField(programService, "cancerRepository", cancerRepository);
  }

  void setupPrimarySiteRepository() {
    for (val name: List.of("Blood", "Brain")) {
      val entity = createPrimarySite(name);
        when(primarySiteRepository.getPrimarySiteByName(name)).thenReturn(entity);
    }

    ReflectionTestUtils.setField(programService, "primarySiteRepository", primarySiteRepository);
  }

  private Optional<CancerEntity> createCancerEntity(String name) {
    return Optional.of(new CancerEntity().setId(UUID.randomUUID()).setName(name));
  }

  private Optional<PrimarySiteEntity> createPrimarySite(String name) {
    return Optional.of(new PrimarySiteEntity().setId(UUID.randomUUID()).setName(name));
  }

  @Test
  void inviteUser() {
    programService.inviteUser(programEntity, "user@example.com", "First", "Last", UserRole.ADMIN);
    val invitationCaptor = ArgumentCaptor.forClass(JoinProgramInvite.class);
    verify(invitationRepository).save(invitationCaptor.capture());

    val invitation = invitationCaptor.getValue();
    assertThat(ReflectionTestUtils.getField(invitation, "program")).isEqualTo(programEntity);
    assertThat(ReflectionTestUtils.getField(invitation, "userEmail")).isEqualTo("user@example.com");
    assertThat((LocalDateTime) ReflectionTestUtils.getField(invitation, "createdAt"))
      .as("Creation time is within 5 seconds")
      .isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(5, ChronoUnit.SECONDS));
    assertThat((LocalDateTime) ReflectionTestUtils.getField(invitation, "expiredAt"))
      .as("Expire time should be after 47 hours")
      .isAfter(LocalDateTime.now(ZoneOffset.UTC).plusHours(47));
    assertThat(ReflectionTestUtils.getField(invitation, "status"))
      .as("Status is appending").isEqualTo(JoinProgramInvite.Status.PENDING);
    assertThat(ReflectionTestUtils.getField(invitation, "firstName"))
      .as("First name is first").isEqualTo("First");
    assertThat(ReflectionTestUtils.getField(invitation, "lastName"))
      .as("Last name is first").isEqualTo("Last");
    assertThat(ReflectionTestUtils.getField(invitation, "role"))
      .as("Role is admin").isEqualTo(UserRole.ADMIN);

    verify(mailService).sendInviteEmail(ArgumentMatchers.any());
  }

  @Test
  void acceptInvitation() {
    when(invitationRepository.findById(invitation.getId())).thenReturn(Optional.of(invitation));
    programService.acceptInvite(invitation.getId());
    verify(egoService).joinProgram(invitation.getUserEmail(), invitation.getProgram(), invitation.getRole());
    verify(invitation).accept();
  }

  @Test
  void createProgram() {
    setup();

    val inputProgramEntity = new ProgramEntity().setName(RandomString.make(10)).setShortName(RandomString.make(33));
    assertThat(inputProgramEntity.getCreatedAt()).isNull();
    assertThat(inputProgramEntity.getUpdatedAt()).isNull();
    when(programConverter.programToProgramEntity(program)).thenReturn(inputProgramEntity);
    val outputEntity = programService.createProgram(program, List.of());
    assertThat(outputEntity.getCreatedAt()).isNotNull();
    assertThat(outputEntity.getUpdatedAt()).isNotNull();
    verify(programRepository).save(inputProgramEntity);
  }

  @Test
  void testMappingProgramEntityToProgram() {
    val entity = new ProgramEntity().
      setCommitmentDonors(1000).
      setCountries("Canada").
      setCreatedAt(LocalDateTime.now()).
      setDescription("Test Program").
      setGenomicDonors(1000).
      setId(UUID.randomUUID()).
      setInstitutions("Institute of Institutions").
      setMembershipType(MembershipType.ASSOCIATE).
      setName("Program One").

      setShortName("P1").
      setSubmittedDonors(1000).
      setUpdatedAt(LocalDateTime.now()).
      setWebsite("http://test.org");

    val egoGroups = getEgoGroups(entity);
    val cancers = getCancerTypes(entity);
    val sites = getSites(entity);

    entity.setProgramCancers(cancers);
    entity.setProgramPrimarySites(sites);
    entity.setEgoGroups(egoGroups);

    val mapper = new ProgramConverterImpl(CommonConverter.INSTANCE);
    val details = mapper.map(entity);
    val program = details.getProgram();
    val metadata = details.getMetadata();
    assertThat(program.getCancerTypesList()).contains("Blood cancer");
  }

  Set<ProgramEgoGroupEntity> getEgoGroups(ProgramEntity entity) {
    val egoGroups = new TreeSet<ProgramEgoGroupEntity>();
    for(val role: UserRole.values()) {
      if (role == UserRole.UNRECOGNIZED) {
        continue;
      }
      egoGroups.add(createGroup(entity, role));
    }
    return egoGroups;
  }

  ProgramEgoGroupEntity createGroup(ProgramEntity entity, UserRole role) {
    return new ProgramEgoGroupEntity().
      setId(UUID.randomUUID()).
      setRole(role).
      setEgoGroupId(UUID.randomUUID()).
      setProgram(entity);
  }

  Set<ProgramCancer> getCancerTypes(ProgramEntity entity) {
    val cancers = new TreeSet<ProgramCancer>();
    val bloodCancer = new CancerEntity().setId(UUID.randomUUID());
    bloodCancer.setName("Blood cancer");
    val c1_id = new ProgramCancerId();
    c1_id.setCancerId(bloodCancer.getId());
    c1_id.setProgramId(entity.getId());

    val c1 = new ProgramCancer();
    c1.setProgram(entity);
    c1.setCancer(bloodCancer);
    c1.setId(c1_id);

    bloodCancer.setProgramCancers(Set.of(c1));

    return cancers;
  }

  Set<ProgramPrimarySite> getSites(ProgramEntity entity) {
    val sites = new TreeSet<ProgramPrimarySite>();
    return sites;
  }


  void getProgram() {
    val id1 ="Program One";
    val id2 = "Program Two";
    val id3 = "Program Three";

    // test success (id found)
    when(programRepository.findByShortName(programEntity.getShortName()))
      .thenReturn(Optional.of(programEntity));
    val result1 = programService.getProgram(id1);
    assertThat(result1).isEqualTo(programEntity);

    // test repository failure (not found/exception condition)
    when(programRepository.findByShortName(id3))
      .thenThrow(new RuntimeException("Repository error"));
    assertThrows(RuntimeException.class, () -> { programService.getProgram(id3); });
  }

  @Test
  void listPrograms() {
    when(programRepository.findAll((Specification<ProgramEntity>) Mockito.any()))
      .thenReturn(List.of(programEntity));
    val programs = programService.listPrograms();
    assertThat(programs).contains(programEntity);
  }

}
