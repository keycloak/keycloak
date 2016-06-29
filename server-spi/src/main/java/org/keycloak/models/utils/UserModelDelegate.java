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

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserModelDelegate implements UserModel {
    protected UserModel delegate;

    public UserModelDelegate(UserModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public void setUsername(String username) {
        delegate.setUsername(username);
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public boolean isOtpEnabled() {
        return delegate.isOtpEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        delegate.setSingleAttribute(name, value);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        delegate.setAttribute(name, values);
    }

    @Override
    public void removeAttribute(String name) {
        delegate.removeAttribute(name);
    }

    @Override
    public String getFirstAttribute(String name) {
        return delegate.getFirstAttribute(name);
    }

    @Override
    public List<String> getAttribute(String name) {
        return delegate.getAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Set<String> getRequiredActions() {
        return delegate.getRequiredActions();
    }

    @Override
    public void addRequiredAction(String action) {
        delegate.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(String action) {
        delegate.removeRequiredAction(action);
    }

    @Override
    public void addRequiredAction(RequiredAction action) {
        delegate.addRequiredAction(action);
    }

    @Override
    public void removeRequiredAction(RequiredAction action) {
        delegate.removeRequiredAction(action);
    }

    @Override
    public String getFirstName() {
        return delegate.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        delegate.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return delegate.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        delegate.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return delegate.getEmail();
    }

    @Override
    public void setEmail(String email) {
        delegate.setEmail(email);
    }

    @Override
    public boolean isEmailVerified() {
        return delegate.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        delegate.setEmailVerified(verified);
    }

    @Override
    public void setOtpEnabled(boolean totp) {
        delegate.setOtpEnabled(totp);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        delegate.updateCredential(cred);
    }

    @Override
    public List<UserCredentialValueModel> getCredentialsDirectly() {
        return delegate.getCredentialsDirectly();
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        delegate.updateCredentialDirectly(cred);
    }

    @Override
    public Set<RoleModel> getRealmRoleMappings() {
        return delegate.getRealmRoleMappings();
    }

    @Override
    public Set<RoleModel> getClientRoleMappings(ClientModel app) {
        return delegate.getClientRoleMappings(app);
    }

    @Override
    public boolean hasRole(RoleModel role) {
        return delegate.hasRole(role);
    }

    @Override
    public void grantRole(RoleModel role) {
        delegate.grantRole(role);
    }

    @Override
    public Set<RoleModel> getRoleMappings() {
        return delegate.getRoleMappings();
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        delegate.deleteRoleMapping(role);
    }

    @Override
    public String getFederationLink() {
        return delegate.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        delegate.setFederationLink(link);
    }

    @Override
    public String getServiceAccountClientLink() {
        return delegate.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        delegate.setServiceAccountClientLink(clientInternalId);
    }

    public UserModel getDelegate() {
        return delegate;
    }
    
    @Override
    public Long getCreatedTimestamp(){
        return delegate.getCreatedTimestamp();
    }
    
    @Override
    public void setCreatedTimestamp(Long timestamp){
        delegate.setCreatedTimestamp(timestamp);
    }

    @Override
    public Set<GroupModel> getGroups() {
        return delegate.getGroups();
    }

    @Override
    public void joinGroup(GroupModel group) {
        delegate.joinGroup(group);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        delegate.leaveGroup(group);

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return delegate.isMemberOf(group);
    }
}
