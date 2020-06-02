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

import javax.validation.Valid;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.val;
import org.junit.jupiter.api.Test;

class TestProgramShortNameValidator {
  ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
  Validator validator = validatorFactory.getValidator();

  @Test
  public void testValidator() {
    // contains invalid characters (numbers)
    validate("ABC123", false);
    // too short
    validate("CA", false);
    // too long
    validate("12345678P-CA", false);
    // must contain dash before country code
    validate("12345678CA", false);
    // not ok (UK is not a valid country code )
    validate("TEST-UK", false);
    // GB is a valid country code (Great Britain)
    // dashes are legal (DK is Denmark)
    validate("---DK", false);
    // lowercase is invalid
    validate("testgb", false);

    validate("9A-FO3-CA", true);
    // underscore is allowed
    validate("9A_FOO3-CA", true);
    // Can only contain numbers
    validate("12345678-CA", true);
  }

  private void validate(String name, boolean ok) {
    int expected = 0;
    if (!ok) {
      expected = 1;
    }
    val name1 = new NameTest(name);
    val constraintViolations = validator.validate(name1);
    assertEquals(expected, constraintViolations.size());
  }
}

@ValidateOnExecution(type = ExecutableType.ALL)
@AllArgsConstructor
@Data
@Valid
class NameTest {
  @ProgramShortName String name;
}
