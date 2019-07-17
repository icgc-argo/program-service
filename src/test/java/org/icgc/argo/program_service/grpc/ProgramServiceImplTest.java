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

package org.icgc.argo.program_service.grpc;

import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.Program;
import org.icgc.argo.program_service.proto.RemoveProgramRequest;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ProgramService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgramServiceImplTest {
  @Mock
  ProgramConverter programConverter;

  @Mock
  CommonConverter commonConverter;

  @Mock
  EgoService egoService;

  @Mock
  ProgramService programService;

  @InjectMocks
  ProgramServiceImpl programServiceImpl;

  @Test
  void createProgram() {
    val request = mock(CreateProgramRequest.class);
    val program = mock(Program.class);
    val responseObserver = mock(StreamObserver.class);

    when(request.getProgram()).thenReturn(program);
    when(programService.createProgram(program))
        .thenThrow(new DataIntegrityViolationException("test error"));
    String err = "";
    try {
      programServiceImpl.createProgram(request, responseObserver);
    } catch (DataIntegrityViolationException ex) {
      err=ex.getMessage();
    }
    Assertions.assertThat(err).isEqualTo("test error");
  }

  @Test
  void removeProgram() {
    val request = mock(RemoveProgramRequest.class);
    when(request.getProgramShortName()).thenReturn(StringValue.of("TEST-XYZ123"));
    val responseObserver = mock(StreamObserver.class);

    doAnswer(invocation -> {
      throw new EmptyResultDataAccessException(1);
    }).when(programService).removeProgram(any(String.class));

    String msg = "";
      try {
        programServiceImpl.removeProgram(request, responseObserver);
      } catch (Exception ex) {
        msg = ex.getMessage();
      }

      Assertions.assertThat(msg).
        isEqualTo("NOT_FOUND: Incorrect result size: expected 1, actual 0");

  }
}