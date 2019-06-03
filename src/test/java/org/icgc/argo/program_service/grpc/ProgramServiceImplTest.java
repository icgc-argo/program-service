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
import org.icgc.argo.program_service.CreateProgramRequest;
import org.icgc.argo.program_service.Program;
import org.icgc.argo.program_service.RemoveProgramRequest;
import org.icgc.argo.program_service.converter.CommonConverter;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.services.EgoService;
import org.icgc.argo.program_service.services.ProgramService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.UUID;

import static org.mockito.Mockito.*;

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
    when(programService.createProgram(program)).thenThrow(new DataIntegrityViolationException("test error"));

    programServiceImpl.createProgram(request, responseObserver);
    val argument = ArgumentCaptor.forClass(Exception.class);
    verify(responseObserver).onError(argument.capture());
    Assertions.assertThat(argument.getValue().getMessage()).as("Capture the error message").contains("test error");
  }

  @Test
  void removeProgram() {
    val request = mock(RemoveProgramRequest.class);
    when(request.getProgramId()).thenReturn(StringValue.of("program-id"));
    when(commonConverter.stringToUUID(any(StringValue.class))).thenReturn(UUID.randomUUID());
    val responseObserver = mock(StreamObserver.class);

    doAnswer(invocation -> {
      throw new EmptyResultDataAccessException(1);
    }).when(programService).removeProgram(any(UUID.class));

    programServiceImpl.removeProgram(request, responseObserver);

    val argument = ArgumentCaptor.forClass(StatusRuntimeException.class);
    verify(responseObserver).onError(argument.capture());
    Assertions.assertThat(argument.getValue().getStatus().getCode()).as("Capture non exist exception").isEqualTo(Status.NOT_FOUND.getCode());
  }
}