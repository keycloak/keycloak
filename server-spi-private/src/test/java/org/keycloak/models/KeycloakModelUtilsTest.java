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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RealmModelDelegate;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    // Tests that count of client lookups during KeycloakModelUtils.getRoleFromString is limited (to prevent issues like DoS or OOM in case that incorrect configuration of the role mapper was provided)
    @Test
    public void testLimitCountOfClientLookupsDuringGetRoleFromString() {
        AtomicInteger counter = new AtomicInteger(0);

        RealmModel realm = new RealmModelDelegate(null) {

            @Override
            public ClientModel getClientByClientId(String clientId) {
                counter.incrementAndGet();
                return null;
            }
        };

        String badRoleName = ".";
        for (int i = 0 ; i < 16 ; i++) {
            badRoleName = badRoleName + badRoleName;
        }
        Assert.assertEquals(65536, badRoleName.length());

        Assert.assertNull(KeycloakModelUtils.getRoleFromString(realm, badRoleName));
        Assert.assertEquals(KeycloakModelUtils.MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE, counter.get());
    }

    @Test
    public void testSplitEscapedPath() {
        assertArrayEquals(new String[]{"parent", "child"}, KeycloakModelUtils.splitPath("/parent/child", true));
        assertArrayEquals(new String[]{"parent/slash", "child"}, KeycloakModelUtils.splitPath("/parent~/slash/child", true));
        assertArrayEquals(new String[]{"parent/slash", "child/slash"}, KeycloakModelUtils.splitPath("/parent~/slash/child~/slash", true));
        assertArrayEquals(new String[]{"parent~/slash", "child/slash"}, KeycloakModelUtils.splitPath("/parent~~/slash/child~/slash", true));

        assertArrayEquals(new String[]{"parent", "child"}, KeycloakModelUtils.splitPath("/parent/child", false));
        assertArrayEquals(new String[]{"parent~", "slash", "child"}, KeycloakModelUtils.splitPath("/parent~/slash/child", false));
        assertArrayEquals(new String[]{"parent~", "slash", "child~", "slash"}, KeycloakModelUtils.splitPath("/parent~/slash/child~/slash", false));
        assertArrayEquals(new String[]{"parent~~", "slash", "child~", "slash"}, KeycloakModelUtils.splitPath("/parent~~/slash/child~/slash", false));
    }

    @Test
    public void testBuildGroupPath() {
        GroupAdapterTest.escapeSlashes = true;
        GroupModel group = new GroupAdapterTest("child", new GroupAdapterTest("parent", null));
        assertEquals("/parent/child", KeycloakModelUtils.buildGroupPath(group));
        group = new GroupAdapterTest("child/slash", new GroupAdapterTest("parent/slash", null));
        assertEquals("/parent~/slash/child~/slash", KeycloakModelUtils.buildGroupPath(group));
        group = new GroupAdapterTest("child/slash", new GroupAdapterTest("parent~/slash", null));
        assertEquals("/parent~~/slash/child~/slash", KeycloakModelUtils.buildGroupPath(group));

        GroupAdapterTest.escapeSlashes = false;
        group = new GroupAdapterTest("child", new GroupAdapterTest("parent", null));
        assertEquals("/parent/child", KeycloakModelUtils.buildGroupPath(group));
        group = new GroupAdapterTest("child/slash", new GroupAdapterTest("parent/slash", null));
        assertEquals("/parent/slash/child/slash", KeycloakModelUtils.buildGroupPath(group));
        group = new GroupAdapterTest("child/slash", new GroupAdapterTest("parent~/slash", null));
        assertEquals("/parent~/slash/child/slash", KeycloakModelUtils.buildGroupPath(group));
    }

    private static void assertParsedRoleQualifier(String[] clientIdAndRoleName, String expectedClientId,
            String expectedRoleName) {

        assertThat(clientIdAndRoleName, arrayWithSize(2));

        String clientId = clientIdAndRoleName[0];
        assertEquals(expectedClientId, clientId);
        String roleName = clientIdAndRoleName[1];
        assertEquals(expectedRoleName, roleName);
    }

    private static class GroupAdapterTest implements GroupModel {

        static boolean escapeSlashes = false;

        private String name;
        private String description;
        private GroupModel parent;

        public GroupAdapterTest(String name, GroupModel parent) {
            this.name = name;
            this.parent = parent;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public void setSingleAttribute(String name, String value) {
        }

        @Override
        public void setAttribute(String name, List<String> values) {
        }

        @Override
        public void removeAttribute(String name) {
        }

        @Override
        public String getFirstAttribute(String name) {
            return null;
        }

        @Override
        public Stream<String> getAttributeStream(String name) {
            return null;
        }

        @Override
        public Map<String, List<String>> getAttributes() {return null;
        }

        @Override
        public GroupModel getParent() {
            return parent;
        }

        @Override
        public String getParentId() {
            return parent.getId();
        }

        @Override
        public Stream<GroupModel> getSubGroupsStream() {
            return Stream.empty();
        }

        @Override
        public void setParent(GroupModel group) {
            this.parent = group;
        }

        @Override
        public void addChild(GroupModel subGroup) {
        }

        @Override
        public void removeChild(GroupModel subGroup) {
        }

        @Override
        public Stream<RoleModel> getRealmRoleMappingsStream() {
            return Stream.empty();
        }

        @Override
        public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
            return Stream.empty();
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return false;
        }

        @Override
        public void grantRole(RoleModel role) {
        }

        @Override
        public Stream<RoleModel> getRoleMappingsStream() {
            return Stream.empty();
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {
        }

        @Override
        public boolean escapeSlashesInGroupPath() {
            return escapeSlashes;
        }
    }
}
