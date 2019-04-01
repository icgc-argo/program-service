package org.icgc.argo.program_service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class Utils {
  public static PublicKey getPublicKey(String key, String algorithm) {
    key = key.replace("\n", "");
    key = key.replace("\r", "");
    key = key.replace("-----BEGIN PUBLIC KEY-----", "");
    key = key.replace("-----END PUBLIC KEY-----", "");
    key = key.replace(" ", "");
    PublicKey publicKey = null;
    try {
      val keyBytes = Base64.getDecoder().decode(key);
      KeyFactory kf = KeyFactory.getInstance(algorithm);
      EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
      publicKey = kf.generatePublic(keySpec);
    } catch (NoSuchAlgorithmException e) {
      log.error("Could not reconstruct the public key, the given algorithm could not be found.");
    } catch (InvalidKeySpecException | IllegalArgumentException e) {
      log.error("Could not reconstruct the public key");
    }

    return publicKey;
  }

  public static String toString(InputStream in) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    StringBuilder result = new StringBuilder();
    String line;
    while((line = reader.readLine()) != null) {
      result.append(line);
    }
    return result.toString();
  }
}
