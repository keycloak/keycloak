/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.federation.kerberos;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosPrincipal {

    private static final char REALM_SEPARATOR = '@';
    private static final char ESCAPE_CHAR = '\\';

    // Full principal name like "john@KEYCLOAK.ORG"
    private final String kerberosPrincipal;
    private final String prefix; // Something like "john"
    private final String realm; // Something like "KEYCLOAK.ORG"
    public KerberosPrincipal(String kerberosPrincipal) {
        int separatorIndex = findRealmSeparatorIndex(kerberosPrincipal);
        if (separatorIndex < 0 || separatorIndex == kerberosPrincipal.length() - 1) {
            throw new IllegalArgumentException("Kerberos principal '" + kerberosPrincipal + "' not valid");
        }
        this.prefix = kerberosPrincipal.substring(0, separatorIndex);
        this.realm = kerberosPrincipal.substring(separatorIndex + 1).toUpperCase();
        this.kerberosPrincipal = prefix + REALM_SEPARATOR + realm;
    }

    /**
     * Finds the index of the single unescaped '{@code @}' separating the principal name from the
     * realm. An '{@code @}' inside the principal name is escaped as "{@code \@}" (RFC 1964, section
     * 2.1.1) and is not treated as the realm separator. Returns {@code -1} when the principal does
     * not contain exactly one unescaped '{@code @}', i.e. it is not a valid {@code name@REALM}.
     */
    private static int findRealmSeparatorIndex(String principal) {
        int separatorIndex = -1;
        for (int i = 0; i < principal.length(); i++) {
            if (principal.charAt(i) == REALM_SEPARATOR && !isEscaped(principal, i)) {
                if (separatorIndex != -1) {
                    // more than one unescaped '@' -> ambiguous, not a valid principal
                    return -1;
                }
                separatorIndex = i;
            }
        }
        return separatorIndex;
    }

    // A character is escaped when preceded by an odd number of consecutive escape characters.
    private static boolean isEscaped(String value, int index) {
        int backslashes = 0;
        for (int i = index - 1; i >= 0 && value.charAt(i) == ESCAPE_CHAR; i--) {
            backslashes++;
        }
        return backslashes % 2 != 0;
    }

    public String getKerberosPrincipal() {
        return kerberosPrincipal;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getRealm() {
        return realm;
    }

    @Override
    public String toString() {
        return this.kerberosPrincipal;
    }
}
