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
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.repositories.*;
import org.icgc.argo.program_service.utils.EntityGenerator;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
class ProgramServiceTest {

  @InjectMocks
  private ProgramService programService;

  @Mock
  private Program program;

  @Mock
  private ProgramEntity programEntity;

  @Mock
  private ProgramRepository programRepository;

  @Mock
  private CancerRepository cancerRepository;

  @Mock
  private PrimarySiteRepository primarySiteRepository;

  @Mock
  private ProgramConverter programConverter;

  @Mock
  ProgramCancerRepository programCancerRepository;

  @Mock
  ProgramPrimarySiteRepository programPrimarySiteRepository;

//  @Autowired
//  private EntityGenerator entityGenerator;

  void setup() {
    program = Program.newBuilder().
      addAllCancerTypes(List.of("Blood cancer", "Brain cancer")).
      addAllPrimarySites(List.of("Blood", "Brain")).
      build();
  }

  @Test
  void createProgram() {
    setup();

    val inputProgramEntity = new ProgramEntity().setName(RandomString.make(10)).setShortName(RandomString.make(33));
    assertThat(inputProgramEntity.getCreatedAt()).isNull();
    assertThat(inputProgramEntity.getUpdatedAt()).isNull();
    when(programConverter.programToProgramEntity(program)).thenReturn(inputProgramEntity);
    val outputEntity = programService.createProgram(program);
    assertThat(outputEntity.getCreatedAt()).isNotNull();
    assertThat(outputEntity.getUpdatedAt()).isNotNull();
    verify(programRepository).save(inputProgramEntity);
  }

  @Test
  void listPrograms() {
    when(programRepository.findAll((Specification<ProgramEntity>) Mockito.any()))
      .thenReturn(List.of(programEntity));
    val programs = programService.listPrograms();
    assertThat(programs).contains(programEntity);
  }

}
