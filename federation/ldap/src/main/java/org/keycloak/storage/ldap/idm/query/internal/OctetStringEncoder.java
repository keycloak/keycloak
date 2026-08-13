package org.keycloak.storage.ldap.idm.query.internal;

import org.keycloak.storage.ldap.idm.query.EscapeStrategy;

class OctetStringEncoder {

  private final EscapeStrategy fallback;

  OctetStringEncoder(EscapeStrategy fallback) {
    this.fallback = fallback;
  }

  public String encode(Object parameterValue, boolean isBinary) {
    String escaped;
    if (parameterValue instanceof byte[]) {
      escaped = EscapeStrategy.escapeHex((byte[]) parameterValue);
    } else {
      escaped = escapeAsString(parameterValue, isBinary);
    }
    return escaped;
  }

  private String escapeAsString(Object parameterValue, boolean isBinary) {
    String escaped;
    String stringValue = parameterValue.toString();
    if (isBinary) {
      escaped = EscapeStrategy.OCTET_STRING.escape(stringValue);
    } else {
      escaped = fallback.escape(stringValue);
    }
    return escaped;
  }

}
