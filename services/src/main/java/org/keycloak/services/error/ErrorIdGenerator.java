package org.keycloak.services.error;

import java.security.SecureRandom;

public class ErrorIdGenerator {

  private static final char[] ALLOWED_CHARS = "abcdefghjkmnpqrstuvwxyz23456789".toCharArray();
  private static final SecureRandom RANDOM = new SecureRandom();

  public static String generate() {
    StringBuilder sb = new StringBuilder(8);
    for (int i = 0; i < 8; i++) {
      sb.append(ALLOWED_CHARS[RANDOM.nextInt(ALLOWED_CHARS.length)]);
    }
    return sb.toString();
  }
}
