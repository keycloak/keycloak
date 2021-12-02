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

package org.keycloak.models.jpa;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientAttributeEntity;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ProtocolMapperEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

import javax.persistence.EntityManager;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientAdapter implements ClientModel, JpaModel<ClientEntity> {

    protected KeycloakSession session;
    protected RealmModel realm;
    protected EntityManager em;
    protected ClientEntity entity;

    public ClientAdapter(RealmModel realm, EntityManager em, KeycloakSession session, ClientEntity entity) {
        this.session = session;
        this.realm = realm;
        this.em = em;
        this.entity = entity;
    }

    @Override
    public ClientEntity getEntity() {
        return entity;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getDescription() { return entity.getDescription(); }

    @Override
    public void setDescription(String description) { entity.setDescription(description); }

    @Override
    public boolean isEnabled() {
        return entity.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public boolean isAlwaysDisplayInConsole() {
        return entity.isAlwaysDisplayInConsole();
    }

    @Override
    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        entity.setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public boolean isPublicClient() {
        return entity.isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        entity.setPublicClient(flag);
    }

    @Override
    public boolean isFrontchannelLogout() {
        return entity.isFrontchannelLogout();
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        entity.setFrontchannelLogout(flag);
    }

    @Override
    public boolean isFullScopeAllowed() {
        return entity.isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        entity.setFullScopeAllowed(value);
    }

    @Override
    public Set<String> getWebOrigins() {
        Set<String> result = new HashSet<>();
        result.addAll(entity.getWebOrigins());
        return result;
    }



    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        entity.setWebOrigins(webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        entity.getWebOrigins().add(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        entity.getWebOrigins().remove(webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        Set<String> result = new HashSet<>();
        result.addAll(entity.getRedirectUris());
        return result;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        entity.setRedirectUris(redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        entity.getRedirectUris().add(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        entity.getRedirectUris().remove(redirectUri);
    }

    @Override
    public String getClientAuthenticatorType() {
        return entity.getClientAuthenticatorType();
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        entity.setClientAuthenticatorType(clientAuthenticatorType);
    }

    @Override
    public String getSecret() {
        return entity.getSecret();
    }

    @Override
    public void setSecret(String secret) {
        entity.setSecret(secret);
    }

    @Override
    public String getRegistrationToken() {
        return entity.getRegistrationToken();
    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        entity.setRegistrationToken(registrationToken);
    }

    @Override
    public boolean validateSecret(String secret) {
        return MessageDigest.isEqual(secret.getBytes(), entity.getSecret().getBytes());
    }

    @Override
    public int getNotBefore() {
        return entity.getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(notBefore);
    }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        return getScopeMappingsStream().filter(r -> RoleUtils.isRealmRole(r, realm));
    }

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        return entity.getScopeMappingIds().stream()
                .map(realm::getRoleById)
                .filter(Objects::nonNull);
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        entity.getScopeMappingIds().add(role.getId());
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        entity.getScopeMappingIds().remove(role.getId());
    }

    @Override
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        if (!Objects.equals(entity.getProtocol(), protocol)) {
            entity.setProtocol(protocol);
            session.getKeycloakSessionFactory().publish((ClientModel.ClientProtocolUpdatedEvent) () -> ClientAdapter.this);
        }
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String name, String value) {
        entity.getAuthFlowBindings().put(name, value);

    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String name) {
        entity.getAuthFlowBindings().remove(name);
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String name) {
        return entity.getAuthFlowBindings().get(name);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        Map<String, String> copy = new HashMap<>();
        copy.putAll(entity.getAuthFlowBindings());
        return copy;
    }

    @Override
    public void setAttribute(String name, String value) {
        boolean valueUndefined = value == null || "".equals(value.trim());

        for (ClientAttributeEntity attr : entity.getAttributes()) {
            if (attr.getName().equals(name)) {
                // clean up, so that attributes previously set with either a empty or null value are removed
                // we should remove this in future versions so that new clients never store empty/null attributes
                if (valueUndefined) {
                    removeAttribute(name);
                } else {
                    attr.setValue(value);
                }
                return;
            }
        }

        // do not create attributes if empty or null
        if (valueUndefined) {
            return;
        }

        ClientAttributeEntity attr = new ClientAttributeEntity();
        attr.setName(name);
        attr.setValue(value);
        attr.setClient(entity);
        em.persist(attr);
        entity.getAttributes().add(attr);
    }

    @Override
    public void removeAttribute(String name) {
        Iterator<ClientAttributeEntity> it = entity.getAttributes().iterator();
        while (it.hasNext()) {
            ClientAttributeEntity attr = it.next();
            if (attr.getName().equals(name)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getAttribute(String name) {
        return getAttributes().get(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        Map<String, String> attrs = new HashMap<>();
        for (ClientAttributeEntity attr : entity.getAttributes()) {
            attrs.put(attr.getName(), attr.getValue());
        }
        return attrs;
    }

    @Override
    public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        addClientScopes(Collections.singleton(clientScope), defaultScope);
    }

    @Override
    public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        session.clients().addClientScopes(getRealm(), this, clientScopes, defaultScope);
    }

    @Override
    public void removeClientScope(ClientScopeModel clientScope) {
        session.clients().removeClientScope(getRealm(), this, clientScope);
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope) {
        return session.clients().getClientScopes(getRealm(), this, defaultScope);
    }


    public static boolean contains(String str, String[] array) {
        for (String s : array) {
            if (str.equals(s)) return true;
        }
        return false;
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return this.entity.getProtocolMappers().stream()
                .map(entity -> {
                    ProtocolMapperModel mapping = new ProtocolMapperModel();
                    mapping.setId(entity.getId());
                    mapping.setName(entity.getName());
                    mapping.setProtocol(entity.getProtocol());
                    mapping.setProtocolMapper(entity.getProtocolMapper());
                    Map<String, String> config = new HashMap<>();
                    if (entity.getConfig() != null) {
                        config.putAll(entity.getConfig());
                    }
                    mapping.setConfig(config);
                    return mapping;
                })
                .distinct();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (getProtocolMapperByName(model.getProtocol(), model.getName()) != null) {
            throw new ModelDuplicateException("Protocol mapper name must be unique per protocol");
        }
        String id = model.getId() != null ? model.getId() : KeycloakModelUtils.generateId();
        ProtocolMapperEntity entity = new ProtocolMapperEntity();
        entity.setId(id);
        entity.setName(model.getName());
        entity.setProtocol(model.getProtocol());
        entity.setProtocolMapper(model.getProtocolMapper());
        entity.setClient(this.entity);
        entity.setConfig(model.getConfig());

        em.persist(entity);
        this.entity.getProtocolMappers().add(entity);
        return entityToModel(entity);
    }

    protected ProtocolMapperEntity getProtocolMapperEntity(String id) {
        for (ProtocolMapperEntity entity : this.entity.getProtocolMappers()) {
            if (entity.getId().equals(id)) {
                return entity;
            }
        }
        return null;

    }

    protected ProtocolMapperEntity getProtocolMapperEntityByName(String protocol, String name) {
        for (ProtocolMapperEntity entity : this.entity.getProtocolMappers()) {
            if (entity.getProtocol().equals(protocol) && entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;

    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity toDelete = getProtocolMapperEntity(mapping.getId());
        if (toDelete != null) {
            session.users().preRemove(mapping);

            this.entity.getProtocolMappers().remove(toDelete);
            em.remove(toDelete);
        }

    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        ProtocolMapperEntity entity = getProtocolMapperEntity(mapping.getId());
        entity.setProtocolMapper(mapping.getProtocolMapper());
        if (entity.getConfig() == null) {
            entity.setConfig(mapping.getConfig());
        } else {
            entity.getConfig().clear();
            entity.getConfig().putAll(mapping.getConfig());
        }
        em.flush();

    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        ProtocolMapperEntity entity = getProtocolMapperEntity(id);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        ProtocolMapperEntity entity = getProtocolMapperEntityByName(protocol, name);
        if (entity == null) return null;
        return entityToModel(entity);
    }

    protected ProtocolMapperModel entityToModel(ProtocolMapperEntity entity) {
        ProtocolMapperModel mapping = new ProtocolMapperModel();
        mapping.setId(entity.getId());
        mapping.setName(entity.getName());
        mapping.setProtocol(entity.getProtocol());
        mapping.setProtocolMapper(entity.getProtocolMapper());
        Map<String, String> config = new HashMap<String, String>();
        if (entity.getConfig() != null) config.putAll(entity.getConfig());
        mapping.setConfig(config);
        return mapping;
    }

    @Override
    public void updateClient() {
        session.getKeycloakSessionFactory().publish(new ClientModel.ClientUpdatedEvent() {

            @Override
            public ClientModel getUpdatedClient() {
                return ClientAdapter.this;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });
    }

    @Override
    public String getClientId() {
        return entity.getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        entity.setClientId(clientId);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return entity.isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        entity.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public String getManagementUrl() {
        return entity.getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        entity.setManagementUrl(url);
    }

    @Override
    public String getRootUrl() {
        return entity.getRootUrl();
    }

    @Override
    public void setRootUrl(String url) {
        entity.setRootUrl(url);
    }

    @Override
    public String getBaseUrl() {
        return entity.getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        entity.setBaseUrl(url);
    }

    @Override
    public boolean isBearerOnly() {
        return entity.isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        entity.setBearerOnly(only);
    }

    @Override
    public boolean isConsentRequired() {
        return entity.isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        entity.setConsentRequired(consentRequired);
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return entity.isStandardFlowEnabled();
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        entity.setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return entity.isImplicitFlowEnabled();
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        entity.setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return entity.isDirectAccessGrantsEnabled();
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        entity.setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return entity.isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        entity.setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public RoleModel getRole(String name) {
        return session.roles().getClientRole(this, name);
    }

    @Override
    public RoleModel addRole(String name) {
        return session.roles().addClientRole(this, name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return session.roles().addClientRole(this, id, name);
    }

    @Override
    public boolean removeRole(RoleModel roleModel) {
        return session.roles().removeRole(roleModel);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return session.roles().getClientRolesStream(this, null, null);
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer first, Integer max) {
        return session.roles().getClientRolesStream(this, first, max);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return session.roles().searchForClientRolesStream(this, search, first, max);
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isFullScopeAllowed()) return true;

        if (RoleUtils.hasRole(getScopeMappingsStream(), role))
            return true;

        return RoleUtils.hasRole(getRolesStream(), role);
    }

    @Override
    @Deprecated
    public Stream<String> getDefaultRolesStream() {
        return realm.getDefaultRole().getCompositesStream().filter(this::isClientRole).map(RoleModel::getName);
    }

    private boolean isClientRole(RoleModel role) {
        return role.isClientRole() && Objects.equals(role.getContainerId(), this.getId());
    }

    @Override
    @Deprecated
    public void addDefaultRole(String name) {
        realm.getDefaultRole().addCompositeRole(getOrAddRoleId(name));
    }

    private RoleModel getOrAddRoleId(String name) {
        RoleModel role = getRole(name);
        if (role == null) {
            role = addRole(name);
        }
        return role;
    }

    @Override
    @Deprecated
    public void removeDefaultRoles(String... defaultRoles) {
        for (String defaultRole : defaultRoles) {
            realm.getDefaultRole().removeCompositeRole(getRole(defaultRole));
        }
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return entity.getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        entity.setNodeReRegistrationTimeout(timeout);
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return entity.getRegisteredNodes();
    }

    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        Map<String, Integer> currentNodes = getRegisteredNodes();
        currentNodes.put(nodeHost, registrationTime);
        em.flush();
    }

    @Override
    public void unregisterNode(String nodeHost) {
        Map<String, Integer> currentNodes = getRegisteredNodes();
        currentNodes.remove(nodeHost);
        em.flush();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ClientModel)) return false;

        ClientModel that = (ClientModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getClientId(), System.identityHashCode(this));
    }

}
