/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.federation.kerberos;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link KerberosPrincipal} name/realm parsing.
 */
public class KerberosPrincipalTest {

    private static final String PREFIX = "john";
    private static final String REALM = "KEYCLOAK.ORG";
    private static final String PRINCIPAL_SEPARATOR = "@";
    private static final String STANDARD_PRINCIPAL = PREFIX + PRINCIPAL_SEPARATOR + REALM;
    private static final String LOWERCASE_REALM_PRINCIPAL = PREFIX + PRINCIPAL_SEPARATOR + "keycloak.org";
    private static final String ESCAPED_AT_PREFIX = "ssiemel\\@a.com";
    private static final String ESCAPED_AT_PRINCIPAL = ESCAPED_AT_PREFIX + PRINCIPAL_SEPARATOR + REALM;
    private static final String ESCAPED_BACKSLASH_PREFIX = "a\\\\";
    private static final String ESCAPED_BACKSLASH_PRINCIPAL = ESCAPED_BACKSLASH_PREFIX + PRINCIPAL_SEPARATOR + REALM;
    private static final String AMBIGUOUS_PRINCIPAL = "a@b@KEYCLOAK.ORG";
    private static final String MISSING_REALM_PRINCIPAL = PREFIX;
    private static final String EMPTY_REALM_PRINCIPAL = PREFIX + PRINCIPAL_SEPARATOR;

    @Test
    public void testStandardPrincipal() {
        KerberosPrincipal principal = new KerberosPrincipal(STANDARD_PRINCIPAL);

        Assert.assertEquals(PREFIX, principal.getPrefix());
        Assert.assertEquals(REALM, principal.getRealm());
        Assert.assertEquals(STANDARD_PRINCIPAL, principal.getKerberosPrincipal());
        Assert.assertEquals(STANDARD_PRINCIPAL, principal.toString());
    }

    @Test
    public void testRealmIsUpperCased() {
        KerberosPrincipal principal = new KerberosPrincipal(LOWERCASE_REALM_PRINCIPAL);

        Assert.assertEquals(PREFIX, principal.getPrefix());
        Assert.assertEquals(REALM, principal.getRealm());
    }

    // An '@' inside the principal name is escaped as "\@" (RFC 1964, section 2.1.1) and
    // must not be treated as the realm separator.
    @Test
    public void testEscapedAtInPrincipalName() {
        KerberosPrincipal principal = new KerberosPrincipal(ESCAPED_AT_PRINCIPAL);

        Assert.assertEquals(ESCAPED_AT_PREFIX, principal.getPrefix());
        Assert.assertEquals(REALM, principal.getRealm());
        Assert.assertEquals(ESCAPED_AT_PRINCIPAL, principal.getKerberosPrincipal());
    }

    // A '@' preceded by an escaped backslash ("\\@") is a real, unescaped realm separator.
    @Test
    public void testEscapedBackslashBeforeSeparator() {
        KerberosPrincipal principal = new KerberosPrincipal(ESCAPED_BACKSLASH_PRINCIPAL);

        Assert.assertEquals(ESCAPED_BACKSLASH_PREFIX, principal.getPrefix());
        Assert.assertEquals(REALM, principal.getRealm());
    }

    // More than one unescaped '@' is ambiguous and not a valid name@REALM principal.
    @Test
    public void testMultipleUnescapedAtIsInvalid() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new KerberosPrincipal(AMBIGUOUS_PRINCIPAL));
    }

    @Test
    public void testMissingRealmIsInvalid() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new KerberosPrincipal(MISSING_REALM_PRINCIPAL));
    }

    @Test
    public void testEmptyRealmIsInvalid() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new KerberosPrincipal(EMPTY_REALM_PRINCIPAL));
    }
}
