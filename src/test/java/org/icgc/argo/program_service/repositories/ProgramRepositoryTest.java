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

package org.icgc.argo.program_service.repositories;

import lombok.val;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.CancerEntity;
import org.icgc.argo.program_service.model.entity.ProgramEgoGroupEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.services.ego.EgoClient;
import org.icgc.argo.program_service.services.ego.EgoRESTClient;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoGroup;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
@TestPropertySource(properties = {
  "spring.datasource.url=jdbc:postgresql://localhost:5432/program_db",
  "spring.datasource.driverClassName=org.postgresql.Driver",
})
class ProgramRepositoryTest {
  ProgramRepository programRepository;
  CancerRepository cancerRepository;
  PrimarySiteRepository primarySiteRepository;
  ProgramEgoGroupRepository egoGroupRepository;
}
