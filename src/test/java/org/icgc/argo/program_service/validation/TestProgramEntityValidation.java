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

package org.icgc.argo.program_service.validation;

import static junit.framework.TestCase.assertEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.PastOrPresent;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;
import org.icgc.argo.program_service.properties.ValidationProperties;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

@Component
public class TestProgramEntityValidation {
  ValidationProperties properties = new ValidationProperties();
  ValidatorFactory validatorFactory = properties.factory();
  Validator validator = validatorFactory.getValidator();

  @Test
  public void testValidator() {
    val now = LocalDateTime.now(ZoneId.of("UTC"));
    val name1 = new DateTest(now, now.plusHours(12));
    val constraintViolations = validator.validate(name1);
    assertEquals(0, constraintViolations.size());
  }
}

@ValidateOnExecution(type = ExecutableType.ALL)
@AllArgsConstructor
@Data
@Valid
class DateTest {
  @PastOrPresent LocalDateTime updatedAt;
  @FutureOrPresent LocalDateTime expiresAt;
}
