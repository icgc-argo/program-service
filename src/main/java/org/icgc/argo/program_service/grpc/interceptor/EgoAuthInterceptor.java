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

package org.icgc.argo.program_service.grpc.interceptor;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import io.grpc.*;
import lombok.NonNull;
import lombok.val;
import org.icgc.argo.program_service.security.EgoSecurity;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("auth")
public class EgoAuthInterceptor implements AuthInterceptor {

  private final EgoSecurity egoSecurity;

  public static final Context.Key<EgoToken> EGO_TOKEN = Context.key("egoToken");

  public static final Metadata.Key<String> JWT_METADATA_KEY =
      Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);

  @Autowired
  public EgoAuthInterceptor(@NonNull EgoSecurity egoSecurity) {
    this.egoSecurity = egoSecurity;
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    String token = metadata.get(JWT_METADATA_KEY);
    val egoToken = egoSecurity.verifyToken(token);
    Context context = Context.current().withValue(EGO_TOKEN, egoToken.orElse(null));
    return Contexts.interceptCall(context, call, metadata, next);
  }
}
