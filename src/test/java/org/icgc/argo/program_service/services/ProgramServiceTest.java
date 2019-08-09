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
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.repositories.*;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@Ignore("This is a service level unit test only testing the glue")
@SpringBootTest
@RunWith(SpringRunner.class)
class ProgramServiceTest {

  @InjectMocks private ProgramService programService;

  @Mock private Program program;

  @Mock private ProgramRepository programRepository;

  @Mock private CancerRepository cancerRepository;

  @Mock private PrimarySiteRepository primarySiteRepository;

  @Mock private ProgramConverter programConverter;

  @Mock private ProgramCancerRepository programCancerRepository;

  @Mock private ProgramPrimarySiteRepository programPrimarySiteRepository;

  @Mock private ProgramInstitutionRepository programInstitutionRepository;

  @Mock private ProgramCountryRepository programCountryRepository;

  @Mock private ProgramRegionRepository programRegionRepository;

  @Mock private InstitutionRepository institutionRepository;

  @Mock private RegionRepository regionRepository;

  @Mock private CountryRepository countryRepository;

  @Test
  void listPrograms() {
    val programEntity = mock(ProgramEntity.class);
    when(programRepository.findAll((Specification<ProgramEntity>) Mockito.any()))
      .thenReturn(List.of(programEntity));
    val programs = programService.listPrograms();
    assertTrue(programs.contains(programEntity));
  }

}
