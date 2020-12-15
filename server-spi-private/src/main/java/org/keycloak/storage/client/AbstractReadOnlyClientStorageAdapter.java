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
package org.keycloak.storage.client;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.storage.ReadOnlyException;

import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractReadOnlyClientStorageAdapter extends AbstractClientStorageAdapter {
    public AbstractReadOnlyClientStorageAdapter(KeycloakSession session, RealmModel realm, ClientStorageProviderModel component) {
        super(session, realm, component);
    }

    @Override
    public void setClientId(String clientId) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setName(String name) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setDescription(String description) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setEnabled(boolean enabled) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void addWebOrigin(String webOrigin) {
        throw new ReadOnlyException("client is read only for this update");
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void addRedirectUri(String redirectUri) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setManagementUrl(String url) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setRootUrl(String url) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setBaseUrl(String url) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setBearerOnly(boolean only) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {

        throw new ReadOnlyException("client is read only for this update");
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setSecret(String secret) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setProtocol(String protocol) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setAttribute(String name, String value) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void removeAttribute(String name) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setPublicClient(boolean flag) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        throw new ReadOnlyException("client is read only for this update");
    }

    @Override
    public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        throw new ReadOnlyException("client is read only for this update");
    }

    @Override
    public void removeClientScope(ClientScopeModel clientScope) {
        throw new ReadOnlyException("client is read only for this update");
    }

    @Override
    public void setNotBefore(int notBefore) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        throw new ReadOnlyException("client is read only for this update");
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void addScopeMapping(RoleModel role) {
        throw new ReadOnlyException("client is read only for this update");

    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        throw new ReadOnlyException("client is read only for this update");

    }
}
