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

package org.icgc.argo.program_service.services;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;

@Slf4j
public class ProgramServiceTest {

  @Test
  public void testCompareListsGood() {
    val userList = List.of("foo");
    val systemList = List.of("foo");
    ProgramService.compareLists("%s", userList, systemList);
    Assert.assertTrue(true); // Get here with no exception
  }

  @Test
  public void testCompareListsBadInput() {
    val userList = List.of("foo", "bar");
    val systemList = List.of("bar");

    try {
      ProgramService.compareLists("%s", userList, systemList);
    } catch (Exception e) {
      Assert.assertEquals(e.getMessage(), "INVALID_ARGUMENT:  foo");
    }
  }

  @Test
  public void testCompareListsFullMessage() {
    val userList = List.of("Blood", "Brain", "Breast", "Foobar");
    val systemList = List.of("Blood", "Brain", "Breast");
    val errorMessage = "Cannot create program, invalid primary sites provided:%s";
    try {
      ProgramService.compareLists(errorMessage, userList, systemList);
    } catch (Exception e) {
      log.info(e.toString());
      Assert.assertEquals(
          e.getMessage(),
          String.format(
              "INVALID_ARGUMENT: Cannot create program, invalid primary sites provided: %s",
              "Foobar"));
    }
  }
}
