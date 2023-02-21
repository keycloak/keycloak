/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:daniel.fesenmeyer@bosch.io">Daniel Fesenmeyer</a>
 */
public class KeycloakModelUtilsTest {

    @Test
    public void normalizeGroupPath() {
        assertEquals("/test", KeycloakModelUtils.normalizeGroupPath("test"));
        assertEquals("/test/x", KeycloakModelUtils.normalizeGroupPath("test/x/"));
        assertEquals("", KeycloakModelUtils.normalizeGroupPath(""));
        assertNull(KeycloakModelUtils.normalizeGroupPath(null));
    }

    @Test
    public void buildRealmRoleQualifier() {
        assertEquals("realm-role", KeycloakModelUtils.buildRoleQualifier(null, "realm-role"));
    }

    @Test
    public void buildClientRoleQualifier() {
        assertEquals("my.client.id.role-name",
                KeycloakModelUtils.buildRoleQualifier("my.client.id", "role-name"));
    }

    @Test
    public void parseRealmRoleQualifier() {
        String[] clientIdAndRoleName = KeycloakModelUtils.parseRole("realm-role");

        assertParsedRoleQualifier(clientIdAndRoleName, null, "realm-role");
    }

    @Test
    public void parseClientRoleQualifier() {
        String[] clientIdAndRoleName = KeycloakModelUtils.parseRole("my.client.id.role-name");

        assertParsedRoleQualifier(clientIdAndRoleName, "my.client.id", "role-name");
    }

    private static void assertParsedRoleQualifier(String[] clientIdAndRoleName, String expectedClientId,
            String expectedRoleName) {

        assertThat(clientIdAndRoleName, arrayWithSize(2));

        String clientId = clientIdAndRoleName[0];
        assertEquals(expectedClientId, clientId);
        String roleName = clientIdAndRoleName[1];
        assertEquals(expectedRoleName, roleName);
    }

}
