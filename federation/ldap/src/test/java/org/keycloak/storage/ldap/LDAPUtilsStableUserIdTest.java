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

package org.keycloak.storage.ldap;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link LDAPUtils#generateStableUserId(String, String, String)}
 */
public class LDAPUtilsStableUserIdTest {

    private static final String REALM_ID = "6c6d5b18-1f1e-4a1d-8a33-2ff9e2f4fffb";
    private static final String PROVIDER_ID = "a6cd7a5f-6a4b-46a1-b0e0-a1b2c3d4e5f6";
    private static final String LDAP_UUID = "b3e8d4a7-1c2f-4c93-9e6d-4f9f3c9a1c39";

    @Test
    public void testIdIsDeterministicForSameAnchors() {
        String first = LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, LDAP_UUID);
        String second = LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, LDAP_UUID);
        Assert.assertEquals(first, second);
    }

    @Test
    public void testIdIsUuidFormatted() {
        String id = LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, LDAP_UUID);
        Assert.assertNotNull(UUID.fromString(id));
        Assert.assertEquals(36, id.length());
    }

    @Test
    public void testDifferentAnchorsProduceDifferentIds() {
        String id = LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, LDAP_UUID);
        Assert.assertNotEquals(id, LDAPUtils.generateStableUserId("6c6d5b18-1f1e-4a1d-8a33-2ff9e2f4fffc", PROVIDER_ID, LDAP_UUID));
        Assert.assertNotEquals(id, LDAPUtils.generateStableUserId(REALM_ID, "a6cd7a5f-6a4b-46a1-b0e0-a1b2c3d4e5f7", LDAP_UUID));
        Assert.assertNotEquals(id, LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, "b3e8d4a7-1c2f-4c93-9e6d-4f9f3c9a1c3a"));
    }

    @Test
    public void testNonUuidLdapIdsAreSupported() {
        // The LDAP UUID attribute may be any unique attribute, for instance uid or entryDN
        String dnStyle = "uid=jdoe,ou=People,dc=keycloak,dc=org";
        String id = LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, dnStyle);
        Assert.assertNotNull(UUID.fromString(id));
        Assert.assertEquals(id, LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, dnStyle));
        Assert.assertNotEquals(id, LDAPUtils.generateStableUserId(REALM_ID, PROVIDER_ID, "uid=jdoe,ou=People,dc=keycloak,dc=orh"));
    }
}
