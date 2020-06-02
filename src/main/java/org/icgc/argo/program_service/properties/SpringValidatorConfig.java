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

package org.icgc.argo.program_service.properties;

import java.util.Map;
import javax.validation.ValidatorFactory;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

// This class configures hibernate to use get the default ValidationFactory from a Bean called
// "factory",
// instead of using it's own default.
//
// We need this so that our Bean can use a ValidationFactory with a Clock setting that always uses
// UTC time.
//
// See:
// https://stackoverflow.com/questions/50212117/spring-boot-hibernate-custom-constraint-doesnt-inject-service/50213178#50213178
// and https://stackoverflow.com/questions/2712345/jsr-303-dependency-injection-and-hibernate
// for more details on how this configuration works.
//
@Configuration
@Lazy
class SpringValidatorConfiguration {
  @Bean
  @Lazy
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
      final ValidatorFactory factory) {
    return new HibernatePropertiesCustomizer() {

      @Override
      public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("javax.persistence.validation.factory", factory);
      }
    };
  }
}
