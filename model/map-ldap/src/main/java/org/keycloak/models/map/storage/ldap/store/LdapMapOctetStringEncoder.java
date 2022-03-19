/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.map.storage.ldap.store;

public class LdapMapOctetStringEncoder {

  private final LdapMapEscapeStrategy fallback;

  public LdapMapOctetStringEncoder() {
    this(null);
  }

  public LdapMapOctetStringEncoder(LdapMapEscapeStrategy fallback) {
    this.fallback = fallback;
  }


  public String encode(Object parameterValue, boolean isBinary) {
    String escaped;
    if (parameterValue instanceof byte[]) {
      escaped = LdapMapEscapeStrategy.escapeHex((byte[]) parameterValue);
    } else {
      escaped = escapeAsString(parameterValue, isBinary);
    }
    return escaped;
  }

  private String escapeAsString(Object parameterValue, boolean isBinary) {
    String escaped;
    String stringValue = parameterValue.toString();
    if (isBinary) {
      escaped = LdapMapEscapeStrategy.OCTET_STRING.escape(stringValue);
    } else if (fallback == null){
      escaped = stringValue;
    } else {
      escaped = fallback.escape(stringValue);
    }
    return escaped;
  }

}
