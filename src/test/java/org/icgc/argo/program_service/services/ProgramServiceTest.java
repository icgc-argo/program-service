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

package org.icgc.argo.program_service.services;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.List;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
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
      Assert.assertEquals(e.getMessage(), String.format("INVALID_ARGUMENT: Cannot create program, invalid primary sites provided: %s", "Foobar"));
    }
  }

}
