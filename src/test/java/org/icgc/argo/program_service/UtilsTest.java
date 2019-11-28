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

package org.icgc.argo.program_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import org.icgc.argo.program_service.proto.MembershipType;
import org.icgc.argo.program_service.proto.MembershipTypeValue;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.proto.UserRoleValue;
import org.junit.jupiter.api.Test;

public class UtilsTest {
  @Test
  void getPublicKey() {
    PublicKey rsa =
        Utils.getPublicKey(
            "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0lOqMuPLCVusc6szklNXQL1FHhSkEgR7An+8BllBqTsRHM4bRYosseGFCbYPn8r8FsWuMDtxp0CwTyMQR2PCbJ740DdpbE1KC6jAfZxqcBete7gP0tooJtbvnA6X4vNpG4ukhtUoN9DzNOO0eqMU0Rgyy5HjERdYEWkwTNB30i9I+nHFOSj4MGLBSxNlnuo3keeomCRgtimCx+L/K3HNo0QHTG1J7RzLVAchfQT0lu3pUJ8kB+UM6/6NG+fVyysJyRZ9gadsr4gvHHckw8oUBp2tHvqBEkEdY+rt1Mf5jppt7JUV7HAPLB/qR5jhALY2FX/8MN+lPLmb/nLQQichVQIDAQAB-----END PUBLIC KEY-----",
            "RSA");
    assertNotNull(rsa);
    assertNull(Utils.getPublicKey("wrongkey", "rsa"), "wrong key should return null");
  }

  @Test
  void streamToString() throws IOException {
    String s = "test123";
    assertEquals(Utils.toString(new ByteArrayInputStream(s.getBytes())), s);
  }

  public static StringValue stringValue(String s) {
    return StringValue.of(s);
  }

  public static Int32Value int32Value(int i) {
    return Int32Value.of(i);
  }

  public static UserRoleValue userRoleValue(UserRole u) {
    return UserRoleValue.newBuilder().setValue(u).build();
  }

  public static MembershipTypeValue membershipTypeValue(MembershipType m) {
    return MembershipTypeValue.newBuilder().setValue(m).build();
  }
}
