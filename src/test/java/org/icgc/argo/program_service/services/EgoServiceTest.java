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

import lombok.val;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.Utils;
import org.icgc.argo.program_service.converter.ProgramConverter;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.repositories.ProgramEgoGroupRepository;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EgoServiceTest {
  @Autowired RestTemplate restTemplate;
  @Test
  void verifyKey() {
    val rsaPublicKey = (RSAPublicKey) Utils.getPublicKey(publickKey, "RSA");
    val programEgoGroupRepository = mock(ProgramEgoGroupRepository.class);

    val retryTemplate = new RetryTemplate();
    val programConverter = mock(ProgramConverter.class);
    val egoClient = mock(EgoRESTClient.class);
    val egoService = new EgoService(programEgoGroupRepository, programConverter, egoClient);
    ReflectionTestUtils.setField(egoService,"egoPublicKey", rsaPublicKey);

    assertTrue(egoService.verifyToken(validToken).isPresent(), "Valid token should return an ego token");
    assertFalse(egoService.verifyToken(expiredToken).isPresent(), "Expired token should return empty ego token");
    assertFalse(egoService.verifyToken(wrongIssToken).isPresent(), "Wrong issuer token should return empty ego token");
    assertTrue(egoService.verifyToken(hasExtraFieldToken).isPresent(), "Return ego token when a token contains an unrecognized field");
  }

  // exp 2053872034
  String validToken = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MjA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6ImQ4NjYwMDkxQGdtYWlsLmNvbSIsImVtYWlsIjoiZDg2NjAwOTFAZ21haWwuY29tIiwic3RhdHVzIjoiQVBQUk9WRUQiLCJmaXJzdE5hbWUiOiJYdSIsImxhc3ROYW1lIjoiRGVuZyIsImNyZWF0ZWRBdCI6MTU1MjQ5MzMzMDYwNywibGFzdExvZ2luIjoxNTUzNzg1NjM0NjE3LCJwcmVmZXJyZWRMYW5ndWFnZSI6bnVsbCwidHlwZSI6IlVTRVIiLCJwZXJtaXNzaW9ucyI6W119fSwic2NvcGUiOltdfQ.Nij4acYzKvXF6CjIxkcXaqNpYSFY_MfmZapAznc1Jh3QE9eYfrjqjaX1VtSybAancHkWwlQsMFyIp7WgcojoWFiLSJ8E4bArLcAUiYbOIh19jGuHpsvSSQVvfjxwyGTG19Gdd2A3XuED80tR1If8ROwq5N85CQXaRhTok1mDqtYqwqvfsAoZMgb8PyjRzbmfYEzEYYOruOfB30y2cpqtFTXGxD21dZAZPtQ4diNR9W0wp-Y1Aq9PJFNlODnZoIuk4zzeq-slsgmuFZtFYp_sy4bpEUPD_gArnMratiD3eoyOfBPREvQkGfYc5z48q480IYO8nRqoVB4Mjr4o0vaTMg";

  String expiredToken = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MTA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6ImQ4NjYwMDkxQGdtYWlsLmNvbSIsImVtYWlsIjoiZDg2NjAwOTFAZ21haWwuY29tIiwic3RhdHVzIjoiQVBQUk9WRUQiLCJmaXJzdE5hbWUiOiJYdSIsImxhc3ROYW1lIjoiRGVuZyIsImNyZWF0ZWRBdCI6MTU1MjQ5MzMzMDYwNywibGFzdExvZ2luIjoxNTUzNzg1NjM0NjE3LCJwcmVmZXJyZWRMYW5ndWFnZSI6bnVsbCwidHlwZSI6IlVTRVIiLCJwZXJtaXNzaW9ucyI6W119fSwic2NvcGUiOltdfQ.QWCosvLeLHC3aSZLpfGvxabM9kwXW8Rxu7aLdVUpSjb150yEfIx_8007nZfXbWQqGHLlOpqJC47df6Ov4MqH3sKTiVDX1N8rMs33AeZVOSvp6s8uw32RgORwMXISrLk4D8aSyW5OW9E7BmPw_vx4Xom9R4i5yeDY-sgKOHMbecF2IpCTqbzHcMHBsgK4pmwUnrCjGtH2e5DIh9EyruI92ZMk9q1OASWhYmKFHCTi6174Bm5xROqwDDTvnbnJwWuw6VIVZVq19xUXq0uLnyY0aR3lK-IVZ3FRQIPrCyusszi5lBf6Y265InC7PwkrpX49vkl8_ztkQRUCIJHSyFIzvw";

  String wrongIssToken = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MTA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoidGVzdCIsImF1ZCI6W10sImp0aSI6IjgyNGMyODM0LTdlZTMtNGU0Mi04MGNhLTNkODZkZDExYTg1ZCIsImNvbnRleHQiOnsic2NvcGUiOltdLCJ1c2VyIjp7Im5hbWUiOiJkODY2MDA5MUBnbWFpbC5jb20iLCJlbWFpbCI6ImQ4NjYwMDkxQGdtYWlsLmNvbSIsInN0YXR1cyI6IkFQUFJPVkVEIiwiZmlyc3ROYW1lIjoiWHUiLCJsYXN0TmFtZSI6IkRlbmciLCJjcmVhdGVkQXQiOjE1NTI0OTMzMzA2MDcsImxhc3RMb2dpbiI6MTU1Mzc4NTYzNDYxNywicHJlZmVycmVkTGFuZ3VhZ2UiOm51bGwsInR5cGUiOiJVU0VSIiwicGVybWlzc2lvbnMiOltdfX0sInNjb3BlIjpbXX0.hJIgblrwL3durICn021DZWJyiH_f-_ZxQSb-LVrx19gT3OnO0tizbCwGejJwBXWyhZGXQ2dWxZo7iloZovKEit5AvJxnQaL0MvIRnMVnjhA0ChZp2jHrIHbHVFvf1PwY0dO2dh-iwHwgBQb9TR0IvNa_2LOC85k71wnMG_WYBwf9K1ww08GgrE9_aTFem8B7EnmOR8Kkgf2JuqSluyc87f--a87fjFKhroNcEUM3NLp_z3qCp-_Fjj_k5LTUqfqG5XdlN9b8eKydfacPxS_KidQpGA7B2KQYUOhJxwYeJIKnTHeE0zB1kMhKuAZKLjqhsFW1XHCJzw0i1mQ_V9jwHA";

  String hasExtraFieldToken = "eyJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1NTM3ODU2MzQsImV4cCI6MjA1Mzg3MjAzNCwic3ViIjoiY2MwODM2ZjktNzMyNC00MzAxLWE3ZDUtZDY5ODUwNTY1OWMyIiwiaXNzIjoiZWdvIiwiYXVkIjpbXSwianRpIjoiODI0YzI4MzQtN2VlMy00ZTQyLTgwY2EtM2Q4NmRkMTFhODVkIiwiY29udGV4dCI6eyJzY29wZSI6W10sInVzZXIiOnsibmFtZSI6ImQ4NjYwMDkxQGdtYWlsLmNvbSIsImVtYWlsIjoiZDg2NjAwOTFAZ21haWwuY29tIiwic3RhdHVzIjoiQVBQUk9WRUQiLCJmaXJzdE5hbWUiOiJYdSIsImxhc3ROYW1lIjoiRGVuZyIsImNyZWF0ZWRBdCI6MTU1MjQ5MzMzMDYwNywibGFzdExvZ2luIjoxNTUzNzg1NjM0NjE3LCJwcmVmZXJyZWRMYW5ndWFnZSI6bnVsbCwidHlwZSI6IlVTRVIiLCJwZXJtaXNzaW9ucyI6W10sImV4dHJhNDIzMTQiOltdfX0sInNjb3BlIjpbXX0.TvjY_kFhvKAh0j308_HpTsofVixoEZBMsZBBNlB77WoPZtimzkfYd9LsdtB3EUrcsDp1uJtFvoJZsNX1mRlS3Y0JH8CFYAY5XKqrcHsaPWPFd4UOMYvz5O0ytxz172xh3LzzCEZOmGzkgi1meCIUaZUv22POPKSz0ygudact1qd7WKieQtAlW51yE49Lhl5r2b-z5wX8VUCfrrQKTlLgxD7FW2WLF-ZfWpGKldxROCrX5eiQtqLmXqX0hAUfyZq9nGeno2V22A2FMWoIHLMA5PX-Sn8Jteju9x1BaPObYBPqmOsm50kSu2sbDY1H1QJtsq0gr831pNH1DGdGCH_2Mg";

  String publickKey = "-----BEGIN PUBLIC KEY----- MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0 e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9 MwIDAQAB -----END PUBLIC KEY-----";

//  String privateKey = "-----BEGIN RSA PRIVATE KEY----- MIIEogIBAAKCAQEAnzyis1ZjfNB0bBgKFMSvvkTtwlvBsaJq7S5wA+kzeVOVpVWw kWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHcaT92whREFpLv9cj5lTeJSibyr/Mr m/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIytvHWTxZYEcXLgAXFuUuaS3uF9gEi NQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0e+lf4s4OxQawWD79J9/5d3Ry0vbV 3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWbV6L11BWkpzGXSW4Hv43qa+GSYOD2 QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9MwIDAQABAoIBACiARq2wkltjtcjs kFvZ7w1JAORHbEufEO1Eu27zOIlqbgyAcAl7q+/1bip4Z/x1IVES84/yTaM8p0go amMhvgry/mS8vNi1BN2SAZEnb/7xSxbflb70bX9RHLJqKnp5GZe2jexw+wyXlwaM +bclUCrh9e1ltH7IvUrRrQnFJfh+is1fRon9Co9Li0GwoN0x0byrrngU8Ak3Y6D9 D8GjQA4Elm94ST3izJv8iCOLSDBmzsPsXfcCUZfmTfZ5DbUDMbMxRnSo3nQeoKGC 0Lj9FkWcfmLcpGlSXTO+Ww1L7EGq+PT3NtRae1FZPwjddQ1/4V905kyQFLamAA5Y lSpE2wkCgYEAy1OPLQcZt4NQnQzPz2SBJqQN2P5u3vXl+zNVKP8w4eBv0vWuJJF+ hkGNnSxXQrTkvDOIUddSKOzHHgSg4nY6K02ecyT0PPm/UZvtRpWrnBjcEVtHEJNp bU9pLD5iZ0J9sbzPU/LxPmuAP2Bs8JmTn6aFRspFrP7W0s1Nmk2jsm0CgYEAyH0X +jpoqxj4efZfkUrg5GbSEhf+dZglf0tTOA5bVg8IYwtmNk/pniLG/zI7c+GlTc9B BwfMr59EzBq/eFMI7+LgXaVUsM/sS4Ry+yeK6SJx/otIMWtDfqxsLD8CPMCRvecC 2Pip4uSgrl0MOebl9XKp57GoaUWRWRHqwV4Y6h8CgYAZhI4mh4qZtnhKjY4TKDjx QYufXSdLAi9v3FxmvchDwOgn4L+PRVdMwDNms2bsL0m5uPn104EzM6w1vzz1zwKz 5pTpPI0OjgWN13Tq8+PKvm/4Ga2MjgOgPWQkslulO/oMcXbPwWC3hcRdr9tcQtn9 Imf9n2spL/6EDFId+Hp/7QKBgAqlWdiXsWckdE1Fn91/NGHsc8syKvjjk1onDcw0 NvVi5vcba9oGdElJX3e9mxqUKMrw7msJJv1MX8LWyMQC5L6YNYHDfbPF1q5L4i8j 8mRex97UVokJQRRA452V2vCO6S5ETgpnad36de3MUxHgCOX3qL382Qx9/THVmbma 3YfRAoGAUxL/Eu5yvMK8SAt/dJK6FedngcM3JEFNplmtLYVLWhkIlNRGDwkg3I5K y18Ae9n7dHVueyslrb6weq7dTkYDi3iOYRW8HRkIQh06wEdbxt0shTzAJvvCQfrB jg/3747WSsf/zBTcHihTRBdAv6OmdhV4/dD5YBfLAkLrd+mX7iE= -----END RSA PRIVATE KEY-----"

  @Test
  void initAdmin_allExistingEmails_success(){
    val egoService1 = mock(EgoService.class);
    val egoClient1 = mock(EgoRESTClient.class);
    ReflectionTestUtils.setField(egoService1,"egoClient", egoClient1);

    val mockProgramEntity = new ProgramEntity();

    val existingEmail = "jon.snow@example.com";
    val nonExistingEmail = "night.king@example.com";
    val erroredEmail = "some.error@example.com";

    // Define behaviour for existing user
    when(egoService1.joinProgram(existingEmail, mockProgramEntity, UserRole.ADMIN)).thenReturn(true);

    // Define behaviour for non-existing
    when(egoService1.joinProgram(nonExistingEmail, mockProgramEntity, UserRole.ADMIN)).thenReturn(false, true);
    when(egoClient1.createEgoUser(nonExistingEmail)).thenReturn(new EgoUser().setStatus("APPROVED").setType("USER").setEmail(nonExistingEmail));

    // Define behaviour for errored user
    when(egoService1.joinProgram(erroredEmail, mockProgramEntity, UserRole.ADMIN)).thenReturn(false, false);
    when(egoClient1.createEgoUser(erroredEmail)).thenThrow(new IllegalStateException(format("Could not create ego user for: %s", erroredEmail )));

    // Indicate the plan to call the real "initAdmin" MUT (method under test) that uses the internal mocked methods
    doCallRealMethod().when(egoService1).initAdmin(existingEmail, mockProgramEntity);
    doCallRealMethod().when(egoService1).initAdmin(nonExistingEmail, mockProgramEntity);
    doCallRealMethod().when(egoService1).initAdmin(erroredEmail, mockProgramEntity);

    // Actually call the MUTs
    egoService1.initAdmin(existingEmail, mockProgramEntity);
    egoService1.initAdmin(nonExistingEmail, mockProgramEntity);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> egoService1.initAdmin(erroredEmail, mockProgramEntity))
        .withMessageStartingWith("Could not create ego user");

    // Verify expected behaviour for existing user case
    verify(egoService1, times(1)).joinProgram(existingEmail, mockProgramEntity, UserRole.ADMIN);
    verify(egoClient1, never()).createEgoUser(existingEmail);

    // Verify expected behaviour for non-existing user case
    verify(egoService1, times(2)).joinProgram(nonExistingEmail, mockProgramEntity, UserRole.ADMIN);
    verify(egoClient1, times(1)).createEgoUser(nonExistingEmail);

    // Verify expected behaviour for errored user case
    verify(egoService1, times(1)).joinProgram(erroredEmail, mockProgramEntity, UserRole.ADMIN);
    verify(egoClient1, times(1)).createEgoUser(erroredEmail);
  }
}
