/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.keycloak.models.file.adapter;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleModel;
import org.keycloak.models.entities.ClientEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.entities.ClientIdentityProviderMappingEntity;
import org.keycloak.models.entities.ProtocolMapperEntity;

/**
 * ClientModel for JSON persistence.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public abstract class ClientAdapter implements ClientModel {

    protected final ClientEntity clientEntity;
    protected final RealmModel realm;
    protected  KeycloakSession session;
    private final RealmProvider model;

    private final Map<String, RoleModel> allScopeMappings = new HashMap<String, RoleModel>();

    public ClientAdapter(KeycloakSession session, RealmModel realm, ClientEntity clientEntity) {
        this.clientEntity = clientEntity;
        this.realm = realm;
        this.session = session;
        this.model = session.realms();
    }

    @Override
    public String getId() {
        return clientEntity.getId();
    }

    @Override
    public String getClientId() {
        return clientEntity.getName();
    }

    @Override
    public long getAllowedClaimsMask() {
        return clientEntity.getAllowedClaimsMask();
    }

    @Override
    public void setAllowedClaimsMask(long mask) {
        clientEntity.setAllowedClaimsMask(mask);
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<String>();
        if (clientEntity.getWebOrigins() != null) {
            result.addAll(clientEntity.getWebOrigins());
        }
        return result;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        List<String> result = new ArrayList<String>();
        result.addAll(webOrigins);
        clientEntity.setWebOrigins(result);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        Set<String> webOrigins = getWebOrigins();
        webOrigins.add(webOrigin);
        setWebOrigins(webOrigins);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        Set<String> webOrigins = getWebOrigins();
        webOrigins.remove(webOrigin);
        setWebOrigins(webOrigins);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<String>();
        if (clientEntity.getRedirectUris() != null) {
            result.addAll(clientEntity.getRedirectUris());
        }
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        List<String> result = new ArrayList<String>();
        result.addAll(redirectUris);
        clientEntity.setRedirectUris(result);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        if (clientEntity.getRedirectUris().contains(redirectUri)) return;
        clientEntity.getRedirectUris().add(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        clientEntity.getRedirectUris().remove(redirectUri);
    }

    @Override
    public boolean isEnabled() {
        return clientEntity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        clientEntity.setEnabled(enabled);
    }

    @Override
    public boolean validateSecret(String secret) {
        return secret.equals(clientEntity.getSecret());
    }

    @Override
    public String getSecret() {
        return clientEntity.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        clientEntity.setSecret(secret);
    }

    @Override
    public boolean isPublicClient() {
        return clientEntity.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        clientEntity.setPublicClient(flag);
    }


    @Override
    public boolean isFrontchannelLogout() {
        return clientEntity.isFrontchannelLogout();
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        clientEntity.setFrontchannelLogout(flag);
    }

    @Override
    public boolean isFullScopeAllowed() {
        return clientEntity.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        clientEntity.setFullScopeAllowed(value);

    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getNotBefore() {
        return clientEntity.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        clientEntity.setNotBefore(notBefore);
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        return new HashSet<RoleModel>(allScopeMappings.values());
    }

    @Override
    public Set<RoleModel> getRealmScopeMappings() {
        Set<RoleModel> allScopes = getScopeMappings();

        Set<RoleModel> realmRoles = new HashSet<RoleModel>();
        for (RoleModel role : allScopes) {
            RoleAdapter roleAdapter = (RoleAdapter)role;
            if (roleAdapter.isRealmRole()) {
                realmRoles.add(role);
            }
        }
        return realmRoles;
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isFullScopeAllowed()) return true;
        Set<RoleModel> roles = getScopeMappings();
        if (roles.contains(role)) return true;

        for (RoleModel mapping : roles) {
            if (mapping.hasRole(role)) return true;
        }
        return false;
    }


    @Override
    public void addScopeMapping(RoleModel role) {
        allScopeMappings.put(role.getId(), role);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        allScopeMappings.remove(role.getId());
    }

    @Override
    public String getProtocol() {
        return clientEntity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        clientEntity.setProtocol(protocol);

    }

    @Override
    public void setAttribute(String name, String value) {
        clientEntity.getAttributes().put(name, value);

    }

    @Override
    public void removeAttribute(String name) {
        clientEntity.getAttributes().remove(name);
    }

    @Override
    public String getAttribute(String name) {
        return clientEntity.getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> copy = new HashMap<String, String>();
        copy.putAll(clientEntity.getAttributes());
        return copy;
    }

    @Override
    public Set<ProtocolMapperModel> getProtocolMappers() {
        Set<ProtocolMapperModel> result = new HashSet<ProtocolMapperModel>();
        for (String id : clientEntity.getProtocolMappers()) {
            ProtocolMapperModel model = getRealm().getProtocolMapperById(id);
            if (model != null) result.add(model);
        }
        return result;
    }

    @Override
    public void addProtocolMappers(Set<String> mapperIds) {
        clientEntity.getProtocolMappers().addAll(mapperIds);
    }

    @Override
    public void removeProtocolMappers(Set<String> mapperIds) {
        clientEntity.getProtocolMappers().removeAll(mapperIds);
    }

    @Override
    public void setProtocolMappers(Set<String> mapperIds) {
        clientEntity.getProtocolMappers().clear();
        clientEntity.getProtocolMappers().addAll(mapperIds);
    }

    @Override
    public void updateAllowedIdentityProviders(List<ClientIdentityProviderMappingModel> identityProviders) {
        List<ClientIdentityProviderMappingEntity> stored = new ArrayList<ClientIdentityProviderMappingEntity>();

        for (ClientIdentityProviderMappingModel model : identityProviders) {
            ClientIdentityProviderMappingEntity entity = new ClientIdentityProviderMappingEntity();

            entity.setId(model.getIdentityProvider());
            entity.setRetrieveToken(model.isRetrieveToken());
            stored.add(entity);
        }

        clientEntity.setIdentityProviders(stored);
    }

    @Override
    public List<ClientIdentityProviderMappingModel> getIdentityProviders() {
        List<ClientIdentityProviderMappingModel> models = new ArrayList<ClientIdentityProviderMappingModel>();

        for (ClientIdentityProviderMappingEntity entity : clientEntity.getIdentityProviders()) {
            ClientIdentityProviderMappingModel model = new ClientIdentityProviderMappingModel();

            model.setIdentityProvider(entity.getId());
            model.setRetrieveToken(entity.isRetrieveToken());

            models.add(model);
        }

        return models;
    }

    @Override
    public boolean hasIdentityProvider(String providerId) {
        for (ClientIdentityProviderMappingEntity identityProviderMappingModel : clientEntity.getIdentityProviders()) {
            String identityProvider = identityProviderMappingModel.getId();

            if (identityProvider.equals(providerId)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isAllowedRetrieveTokenFromIdentityProvider(String providerId) {
        for (ClientIdentityProviderMappingEntity identityProviderMappingModel : clientEntity.getIdentityProviders()) {
            if (identityProviderMappingModel.getId().equals(providerId)) {
                return identityProviderMappingModel.isRetrieveToken();
            }
        }

        return false;
    }

}
