package org.icgc.argo.program_service;


import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {
  @Test
  void getPublicKey() {
    PublicKey rsa = Utils.getPublicKey("-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0lOqMuPLCVusc6szklNXQL1FHhSkEgR7An+8BllBqTsRHM4bRYosseGFCbYPn8r8FsWuMDtxp0CwTyMQR2PCbJ740DdpbE1KC6jAfZxqcBete7gP0tooJtbvnA6X4vNpG4ukhtUoN9DzNOO0eqMU0Rgyy5HjERdYEWkwTNB30i9I+nHFOSj4MGLBSxNlnuo3keeomCRgtimCx+L/K3HNo0QHTG1J7RzLVAchfQT0lu3pUJ8kB+UM6/6NG+fVyysJyRZ9gadsr4gvHHckw8oUBp2tHvqBEkEdY+rt1Mf5jppt7JUV7HAPLB/qR5jhALY2FX/8MN+lPLmb/nLQQichVQIDAQAB-----END PUBLIC KEY-----", "RSA");
    assertNotNull(rsa);
    assertNull(Utils.getPublicKey("wrongkey", "rsa"), "wrong key should return null");
  }

  @Test
  void streamToString() throws IOException {
    String s = "test123";
    assertEquals(Utils.toString(new ByteArrayInputStream(s.getBytes())), s);
  }
}