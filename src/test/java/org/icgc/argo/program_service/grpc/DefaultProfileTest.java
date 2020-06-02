/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.grpc;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import lombok.SneakyThrows;
import lombok.val;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.CreateProgramResponse;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;

// TODO: This test doesn't work, it should be converted to integration test
@ActiveProfiles("default")
public class DefaultProfileTest {

  private Channel channel;

  private String serverName;

  @Mock private EgoService egoService;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUp() {
    // Generate a unique in-process server name.
    serverName = InProcessServerBuilder.generateName();
    // Create a client channel and register for automatic graceful shutdown.
    channel =
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @SneakyThrows
  @Test
  public void egoInterceptorDisabled() {
    val target =
        new ProgramServiceGrpc.ProgramServiceImplBase() {
          public void createProgram(
              CreateProgramRequest request,
              StreamObserver<CreateProgramResponse> responseObserver) {
            responseObserver.onNext(
                CreateProgramResponse.newBuilder().build().getDefaultInstance());
            responseObserver.onCompleted();
          }
        };

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(target)
            .build()
            .start());
    val blockingStub = ProgramServiceGrpc.newBlockingStub(channel);

    // EgoService is not initialized under default profile
    assertNull(egoService);

    // Ego interceptor is not stopping create request, as expected
    val detail = blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
    assertTrue(detail.equals(CreateProgramResponse.newBuilder().build().getDefaultInstance()));
  }
}
