/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol;

import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Alexander Schwartz
 */
public class ProtocolMapperUtilsTest {

    private final UserModel model = new DummyUserModel() {
        @Override
        public String getEmail() {
            return "me@example.com";
        }
    };

    @Test
    public void shouldReadExistingProperty() {
        assertEquals("me@example.com", ProtocolMapperUtils.getUserModelValue(model, "email"));
    }

    @Test
    public void shouldReadExistingDeprecatedProperty() {
        assertEquals("me@example.com", ProtocolMapperUtils.getUserModelValue(model, "Email"));
    }

    @Test
    public void shouldDefaultToNullForNonexistingProperty() {
        assertNull(ProtocolMapperUtils.getUserModelValue(model, "nonexistent"));
    }

    private static class DummyUserModel implements UserModel {

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
            return null;
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
        public Stream<String> getAttributeStream(String name) {
            return null;
        }

        @Override
        public Map<String, List<String>> getAttributes() {
            return null;
        }

        @Override
        public Stream<String> getRequiredActionsStream() {
            return null;
        }

        @Override
        public void addRequiredAction(String action) {

        }

        @Override
        public void removeRequiredAction(String action) {

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
        public Stream<GroupModel> getGroupsStream() {
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

        @Override
        public SubjectCredentialManager credentialManager() {
            return null;
        }

        @Override
        public Stream<RoleModel> getRealmRoleMappingsStream() {
            return null;
        }

        @Override
        public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
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
        public Stream<RoleModel> getRoleMappingsStream() {
            return null;
        }

        @Override
        public void deleteRoleMapping(RoleModel role) {

        }
    }

}