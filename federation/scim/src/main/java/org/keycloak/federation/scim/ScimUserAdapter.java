/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.federation.scim;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Adapter that wraps SCIM User and adapts it to Keycloak UserModel.
 */
public class ScimUserAdapter extends AbstractUserAdapterFederatedStorage {

    private final ScimUser scimUser;

    public ScimUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel storageProviderModel, ScimUser scimUser) {
        super(session, realm, storageProviderModel);
        this.scimUser = scimUser;
    }

    @Override
    public String getId() {
        return StorageId.keycloakId(model, scimUser.getId());
    }

    @Override
    public String getUsername() {
        return scimUser.getUserName();
    }

    @Override
    public void setUsername(String username) {
        scimUser.setUserName(username);
    }

    @Override
    public String getEmail() {
        return scimUser.getEmail();
    }

    @Override
    public void setEmail(String email) {
        scimUser.setEmail(email);
    }

    @Override
    public String getFirstName() {
        return scimUser.getGivenName();
    }

    @Override
    public void setFirstName(String firstName) {
        scimUser.setGivenName(firstName);
    }

    @Override
    public String getLastName() {
        return scimUser.getFamilyName();
    }

    @Override
    public void setLastName(String lastName) {
        scimUser.setFamilyName(lastName);
    }

    @Override
    public boolean isEnabled() {
        return scimUser.isActive();
    }

    @Override
    public void setEnabled(boolean enabled) {
        scimUser.setActive(enabled);
    }

    @Override
    public Long getCreatedTimestamp() {
        return null;
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        // Not supported
    }

    @Override
    public boolean isEmailVerified() {
        return true;
    }

    @Override
    public void setEmailVerified(boolean verified) {
        // Not supported
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = super.getAttributes();
        attrs.put("scimId", Collections.singletonList(scimUser.getId()));
        return attrs;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if ("scimId".equals(name)) {
            return Stream.of(scimUser.getId());
        }
        return super.getAttributeStream(name);
    }

    public ScimUser getScimUser() {
        return scimUser;
    }
}
