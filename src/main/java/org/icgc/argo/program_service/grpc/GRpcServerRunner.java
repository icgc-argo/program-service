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

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.services.HealthStatusManager;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.grpc.interceptor.AuthInterceptor;
import org.icgc.argo.program_service.grpc.interceptor.ExceptionInterceptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.grpcEnabled")
public class GRpcServerRunner implements CommandLineRunner, DisposableBean {

  private Server server;

  private final AuthInterceptor authInterceptor;
  private final ExceptionInterceptor exceptionInterceptor;
  private final ProgramServiceImpl programServiceImpl;
  private final HealthStatusManager healthStatusManager;

  @Value("${app.grpcPort}")
  private Integer port;

  @Autowired
  public GRpcServerRunner(
      ProgramServiceImpl programServiceImpl,
      AuthInterceptor authInterceptor,
      ExceptionInterceptor exceptionInterceptor) {
    this.programServiceImpl = programServiceImpl;
    this.authInterceptor = authInterceptor;
    this.exceptionInterceptor = exceptionInterceptor;
    this.healthStatusManager = new HealthStatusManager();
  }

  @Override
  public void run(String... args) {
    // Interceptor bean depends on run profile.
    val programService =
        ServerInterceptors.intercept(programServiceImpl, authInterceptor, exceptionInterceptor);
    healthStatusManager.setStatus("program_service.ProgramService", ServingStatus.SERVING);

    try {
      server =
          ServerBuilder.forPort(port)
              .addService(programService)
              .addService(ProtoReflectionService.newInstance())
              .addService(healthStatusManager.getHealthService())
              .build()
              .start();
    } catch (IOException e) {
      log.error("gRPC server cannot be started", e);
    }

    log.info("gRPC Server started, listening on port " + port);
    startDaemonAwaitThread();
  }

  private void startDaemonAwaitThread() {
    Thread awaitThread =
        new Thread(
            () -> {
              try {
                this.server.awaitTermination();
              } catch (InterruptedException e) {
                log.error("gRPC server stopped.", e);
              }
            });
    awaitThread.start();
  }

  @Override
  public final void destroy() {
    log.info("Shutting down gRPC server ...");
    Optional.ofNullable(server).ifPresent(Server::shutdown);
    log.info("gRPC server stopped.");
  }
}
