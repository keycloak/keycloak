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
package org.keycloak.storage.adapter;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.federated.UserFederatedStorageProvider;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractUserAdapter implements UserModel {
    protected KeycloakSession session;
    protected RealmModel realm;

    public UserFederatedStorageProvider getFederatedStorage() {
        return null;
    }

    @Override
    public Set<String> getRequiredActions() {
        return getFederatedStorage().getRequiredActions(realm, this);
    }

    @Override
    public void addRequiredAction(String action) {
        getFederatedStorage().addRequiredAction(realm, this, action);

    }

    @Override
    public void removeRequiredAction(String action) {
        getFederatedStorage().removeRequiredAction(realm, this, action);

    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        getFederatedStorage().addRequiredAction(realm, this, action.name());

    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        getFederatedStorage().removeRequiredAction(realm, this, action.name());
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
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public boolean isOtpEnabled() {
        return false;
    }

    @Override
    public void setOtpEnabled(boolean totp) {

    }
}
