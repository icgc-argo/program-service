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

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;

import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.util.Optional;
import lombok.val;
import org.icgc.argo.program_service.grpc.interceptor.EgoAuthInterceptor;
import org.icgc.argo.program_service.proto.CreateProgramRequest;
import org.icgc.argo.program_service.proto.CreateProgramResponse;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc;
import org.icgc.argo.program_service.proto.ProgramServiceGrpc.ProgramServiceImplBase;
import org.icgc.argo.program_service.security.EgoSecurity;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

// GrpcCleanupRule only works with junit 4
@RunWith(MockitoJUnitRunner.class)
public class EgoAuthInterceptorTest {

  @Mock private EgoSecurity egoSecurity;

  @Mock private EgoToken egoToken;

  private Channel channel;

  private String serverName;

  private EgoToken egoTokenSpy;

  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Before
  public void setUp() {
    // Generate a unique in-process server name.
    serverName = InProcessServerBuilder.generateName();
    // Create a client channel and register for automatic graceful shutdown.
    channel =
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
  }

  @Test
  public void egoAuthInterceptor_setEgoToken() throws Exception {
    ProgramServiceImplBase programServiceImplBase =
        new ProgramServiceImplBase() {
          @Override
          public void createProgram(
              CreateProgramRequest request,
              StreamObserver<CreateProgramResponse> responseObserver) {
            EgoAuthInterceptorTest.this.egoTokenSpy = EgoAuthInterceptor.EGO_TOKEN.get();
            responseObserver.onNext(CreateProgramResponse.getDefaultInstance());
            responseObserver.onCompleted();
          }
        };

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
        InProcessServerBuilder.forName(serverName)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    programServiceImplBase, new EgoAuthInterceptor(egoSecurity)))
            .build()
            .start());

    val jwtClientInterceptor = new JwtClientInterceptor();
    val blockingStub =
        ProgramServiceGrpc.newBlockingStub(channel).withInterceptors(jwtClientInterceptor);

    jwtClientInterceptor.token = "123";
    given(egoSecurity.verifyToken("123")).willReturn(Optional.of(egoToken));

    blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
    assertNotNull(this.egoTokenSpy);

    given(egoSecurity.verifyToken("321")).willReturn(Optional.empty());
    jwtClientInterceptor.token = "321";
    blockingStub.createProgram(CreateProgramRequest.getDefaultInstance());
    assertNull(this.egoTokenSpy);
  }

  class JwtClientInterceptor implements ClientInterceptor {
    private String token;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
      return new ForwardingClientCall.SimpleForwardingClientCall<>(
          next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          headers.put(EgoAuthInterceptor.JWT_METADATA_KEY, token);
          super.start(responseListener, headers);
        }
      };
    }
  }
}
