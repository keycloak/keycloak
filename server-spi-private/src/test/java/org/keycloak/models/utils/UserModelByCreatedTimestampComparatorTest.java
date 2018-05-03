/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class UserModelByCreatedTimestampComparatorTest {

    @Test
    public void test() {
        UserModel u1 = new UserModelMock(null);
        UserModel u2 = new UserModelMock(null);
        UserModel u3 = new UserModelMock(150L);
        UserModel u4 = new UserModelMock(1000L);
        
        TreeSet<UserModel> ts = new TreeSet<>(UserModelByCreatedTimestampComparator.INSTANCE);
        
        ts.add(u4);
        ts.add(u1);
        ts.add(u3);
        ts.add(u2);
        
        Iterator<UserModel> i = ts.iterator();
        
        Assert.assertEquals(u3, i.next());
        Assert.assertEquals(u4, i.next());
        Assert.assertEquals(u2, i.next());
        Assert.assertEquals(u1, i.next());

    }

    private static class UserModelMock implements UserModel {

        Long createdTimestamp;
        
        UserModelMock(Long createdTimestamp){
            this.createdTimestamp = createdTimestamp;
        }

        @Override
        public Set<RoleModel> getRealmRoleMappings() {
            return null;
        }

        @Override
        public Set<RoleModel> getClientRoleMappings(ClientModel app) {
            return null;
        }

        @Override
        public boolean hasRole(RoleModel role) {
            return false;
        }

        @Override
        public void grantRole(RoleModel role) {

        }

        @Override
        public Set<RoleModel> getRoleMappings() {

            return null;
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {

        }

        @Override
        public String getId() {

            return null;
        }

        @Override
        public String getUsername() {

            return null;
        }

        @Override
        public void setUsername(String username) {

        }

        @Override
        public Long getCreatedTimestamp() {
            return createdTimestamp;
        }

        @Override
        public void setCreatedTimestamp(Long timestamp) {

        }

        @Override
        public boolean isEnabled() {

            return false;
        }

        @Override
        public void setEnabled(boolean enabled) {

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
        public List<String> getAttribute(String name) {

            return null;
        }

        @Override
        public Map<String, List<String>> getAttributes() {

            return null;
        }

        @Override
        public Set<String> getRequiredActions() {

            return null;
        }

        @Override
        public void addRequiredAction(String action) {

        }

        @Override
        public void removeRequiredAction(String action) {

        }

        @Override
        public void addRequiredAction(RequiredAction action) {

        }

        @Override
        public void removeRequiredAction(RequiredAction action) {

        }

        @Override
        public String getFirstName() {

            return null;
        }

        @Override
        public void setFirstName(String firstName) {

        }

        @Override
        public String getLastName() {

            return null;
        }

        @Override
        public void setLastName(String lastName) {

        }

        @Override
        public String getEmail() {

            return null;
        }

        @Override
        public void setEmail(String email) {

        }

        @Override
        public boolean isEmailVerified() {

            return false;
        }

        @Override
        public void setEmailVerified(boolean verified) {

        }

        @Override
        public Set<GroupModel> getGroups() {

            return null;
        }

        @Override
        public void joinGroup(GroupModel group) {

        }

        @Override
        public void leaveGroup(GroupModel group) {

        }

        @Override
        public boolean isMemberOf(GroupModel group) {

            return false;
        }

        @Override
        public String getFederationLink() {

            return null;
        }

        @Override
        public void setFederationLink(String link) {

        }

        @Override
        public String getServiceAccountClientLink() {

            return null;
        }

        @Override
        public void setServiceAccountClientLink(String clientInternalId) {

        }

    }

}
