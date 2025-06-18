/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.storage.ldap.idm.store.ldap.extended;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.naming.ldap.ExtendedRequest;
import javax.naming.ldap.ExtendedResponse;

/**
 * An implementation of the
 * <a target="_blank" href="https://tools.ietf.org/html/rfc3062">
 * LDAP Password Modify Extended Operation
 * </a>
 * client request.
 * <p>
 * Can be directed at any LDAP server that supports the Password Modify Extended Operation.
 *
 * @author Josh Cummings
 * @since 4.2.9
 */
public final class PasswordModifyRequest implements ExtendedRequest {

  public static final String PASSWORD_MODIFY_OID = "1.3.6.1.4.1.4203.1.11.1";

  private static final byte SEQUENCE_TYPE = 48;
  private static final byte USER_IDENTITY_OCTET_TYPE = -128;
  private static final byte OLD_PASSWORD_OCTET_TYPE = -127;
  private static final byte NEW_PASSWORD_OCTET_TYPE = -126;

  private final ByteArrayOutputStream value = new ByteArrayOutputStream();

  public PasswordModifyRequest(String userIdentity, String oldPassword, String newPassword) {
    ByteArrayOutputStream elements = new ByteArrayOutputStream();

    if (userIdentity != null) {
      berEncode(USER_IDENTITY_OCTET_TYPE, userIdentity.getBytes(), elements);
    }

    if (oldPassword != null) {
      berEncode(OLD_PASSWORD_OCTET_TYPE, oldPassword.getBytes(), elements);
    }

    if (newPassword != null) {
      berEncode(NEW_PASSWORD_OCTET_TYPE, newPassword.getBytes(), elements);
    }

    berEncode(SEQUENCE_TYPE, elements.toByteArray(), this.value);
  }

  @Override
  public String getID() {
    return PASSWORD_MODIFY_OID;
  }

  @Override
  public byte[] getEncodedValue() {
    return this.value.toByteArray();
  }

  @Override
  public ExtendedResponse createExtendedResponse(String id, byte[] berValue, int offset, int length) {
    return null;
  }

  /**
   * Only minimal support for
   * <a target="_blank" href="https://www.itu.int/ITU-T/studygroups/com17/languages/X.690-0207.pdf">
   * BER encoding
   * </a>; just what is necessary for the Password Modify request.
   */
  private void berEncode(byte type, byte[] src, ByteArrayOutputStream dest) {
    int length = src.length;

    dest.write(type);

    if (length < 128) {
      dest.write(length);
    } else if ((length & 0x0000_00FF) == length) {
      dest.write((byte) 0x81);
      dest.write((byte) (length & 0xFF));
    } else if ((length & 0x0000_FFFF) == length) {
      dest.write((byte) 0x82);
      dest.write((byte) ((length >> 8) & 0xFF));
      dest.write((byte) (length & 0xFF));
    } else if ((length & 0x00FF_FFFF) == length) {
      dest.write((byte) 0x83);
      dest.write((byte) ((length >> 16) & 0xFF));
      dest.write((byte) ((length >> 8) & 0xFF));
      dest.write((byte) (length & 0xFF));
    } else {
      dest.write((byte) 0x84);
      dest.write((byte) ((length >> 24) & 0xFF));
      dest.write((byte) ((length >> 16) & 0xFF));
      dest.write((byte) ((length >> 8) & 0xFF));
      dest.write((byte) (length & 0xFF));
    }

    try {
      dest.write(src);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to BER encode provided value of type: " + type);
    }
  }
}
