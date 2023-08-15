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

package org.icgc.argo.program_service.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.model.exceptions.UnauthorizedException;
import org.icgc.argo.program_service.services.ego.Context;
import org.icgc.argo.program_service.services.ego.model.entity.EgoToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile("auth")
public class EgoRestSecurity {

  private static final String EGO = "ego";

  private final RSAPublicKey egoPublicKey;

  @Autowired
  public EgoRestSecurity(@NonNull RSAPublicKey egoPublicKey) {
    this.egoPublicKey = egoPublicKey;
  }

  public Optional<EgoToken> verifyRestTokenHeader(String jwtToken) {
    try {
      Algorithm algorithm = Algorithm.RSA256(this.egoPublicKey, null);
      JWTVerifier verifier =
          JWT.require(algorithm).withIssuer(EGO).build(); // Reusable verifier instance
      if (jwtToken != null) {
        val jwt = verifier.verify(jwtToken);
        return parseToken(jwt);
      } else {
        log.warn("No Token or Invalid Token");
        throw new UnauthorizedException("No Token or Invalid Token");
      }
    } catch (JWTVerificationException | NullPointerException e) {
      log.warn(e.getMessage());
      throw new UnauthorizedException("No Token or Invalid Token");
    }
  }

  private Optional<EgoToken> parseToken(DecodedJWT jwt) {
    try {
      EgoToken egoToken = new EgoToken(jwt, jwt.getClaim("context").as(Context.class));
      return Optional.of(egoToken);
    } catch (JWTDecodeException exception) {
      // Invalid token
      return Optional.empty();
    }
  }
}
