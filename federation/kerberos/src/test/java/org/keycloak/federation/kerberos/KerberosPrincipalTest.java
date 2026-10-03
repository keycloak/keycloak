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

    @Test
    public void testStandardPrincipal() {
        KerberosPrincipal principal = new KerberosPrincipal("john@KEYCLOAK.ORG");

        Assert.assertEquals("john", principal.getPrefix());
        Assert.assertEquals("KEYCLOAK.ORG", principal.getRealm());
        Assert.assertEquals("john@KEYCLOAK.ORG", principal.getKerberosPrincipal());
        Assert.assertEquals("john@KEYCLOAK.ORG", principal.toString());
    }

    @Test
    public void testRealmIsUpperCased() {
        KerberosPrincipal principal = new KerberosPrincipal("john@keycloak.org");

        Assert.assertEquals("john", principal.getPrefix());
        Assert.assertEquals("KEYCLOAK.ORG", principal.getRealm());
    }

    // An '@' inside the principal name is escaped as "\@" (RFC 1964, section 2.1.1) and
    // must not be treated as the realm separator.
    @Test
    public void testEscapedAtInPrincipalName() {
        KerberosPrincipal principal = new KerberosPrincipal("ssiemel\\@a.com@KEYCLOAK.ORG");

        Assert.assertEquals("ssiemel\\@a.com", principal.getPrefix());
        Assert.assertEquals("KEYCLOAK.ORG", principal.getRealm());
        Assert.assertEquals("ssiemel\\@a.com@KEYCLOAK.ORG", principal.getKerberosPrincipal());
    }

    // A '@' preceded by an escaped backslash ("\\@") is a real, unescaped realm separator.
    @Test
    public void testEscapedBackslashBeforeSeparator() {
        KerberosPrincipal principal = new KerberosPrincipal("a\\\\@KEYCLOAK.ORG");

        Assert.assertEquals("a\\\\", principal.getPrefix());
        Assert.assertEquals("KEYCLOAK.ORG", principal.getRealm());
    }

    // More than one unescaped '@' is ambiguous and not a valid name@REALM principal.
    @Test
    public void testMultipleUnescapedAtIsInvalid() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new KerberosPrincipal("a@b@KEYCLOAK.ORG"));
    }

    @Test
    public void testMissingRealmIsInvalid() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new KerberosPrincipal("john"));
    }

    @Test
    public void testEmptyRealmIsInvalid() {
        Assert.assertThrows(IllegalArgumentException.class,
                () -> new KerberosPrincipal("john@"));
    }
}
