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

package org.keycloak.models.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;

/**
 *
 * @author rmartinc
 */
public class KeycloakModelUtilsTest {

    private static class GroupAdapterTest implements GroupModel {

        private String name;
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

    }

    @Test
    public void testSplitEscapedPath() {
        Assert.assertArrayEquals(new String[]{"parent", "child"}, KeycloakModelUtils.splitEscapedPath("/parent/child"));
        Assert.assertArrayEquals(new String[]{"parent/slash", "child"}, KeycloakModelUtils.splitEscapedPath("/parent\\/slash/child"));
        Assert.assertArrayEquals(new String[]{"parent/slash", "child/slash"}, KeycloakModelUtils.splitEscapedPath("/parent\\/slash/child\\/slash"));
        Assert.assertArrayEquals(new String[]{"parent\\/slash", "child/slash"}, KeycloakModelUtils.splitEscapedPath("/parent\\\\/slash/child\\/slash"));
    }

    @Test
    public void testBuildGroupPath() {
        GroupModel group = new GroupAdapterTest("child", new GroupAdapterTest("parent", null));
        Assert.assertEquals("/parent/child", KeycloakModelUtils.buildGroupPath(group));
        group = new GroupAdapterTest("child/slash", new GroupAdapterTest("parent/slash", null));
        Assert.assertEquals("/parent\\/slash/child\\/slash", KeycloakModelUtils.buildGroupPath(group));
        group = new GroupAdapterTest("child/slash", new GroupAdapterTest("parent\\/slash", null));
        Assert.assertEquals("/parent\\\\/slash/child\\/slash", KeycloakModelUtils.buildGroupPath(group));
    }
}
