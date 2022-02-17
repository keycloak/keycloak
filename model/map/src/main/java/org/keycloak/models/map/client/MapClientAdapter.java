/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.client;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public abstract class MapClientAdapter extends AbstractClientModel<MapClientEntity> implements ClientModel {

    private static final Logger LOG = Logger.getLogger(MapClientAdapter.class);
    private final MapProtocolMapperUtils pmUtils;

    public MapClientAdapter(KeycloakSession session, RealmModel realm, MapClientEntity entity) {
        super(session, realm, entity);
        pmUtils = MapProtocolMapperUtils.instanceFor(safeGetProtocol());
    }

    @Override
    public String getId() {
        return entity.getId();
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
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        entity.setName(name);
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        entity.setDescription(description);
    }

    @Override
    public boolean isEnabled() {
        final Boolean enabled = entity.isEnabled();
        return enabled == null ? false : enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        entity.setEnabled(enabled);
    }

    @Override
    public boolean isAlwaysDisplayInConsole() {
        final Boolean alwaysDisplayInConsole = entity.isAlwaysDisplayInConsole();
        return alwaysDisplayInConsole == null ? false : alwaysDisplayInConsole;
    }

    @Override
    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        entity.setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        final Boolean surrogateAuthRequired = entity.isSurrogateAuthRequired();
        return surrogateAuthRequired == null ? false : surrogateAuthRequired;
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        entity.setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public Set<String> getWebOrigins() {
        final Set<String> webOrigins = entity.getWebOrigins();
        return webOrigins == null ? Collections.emptySet() : webOrigins;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        entity.setWebOrigins(webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        entity.addWebOrigin(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        entity.removeWebOrigin(webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        final Set<String> redirectUris = entity.getRedirectUris();
        return redirectUris == null ? Collections.emptySet() : redirectUris;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        entity.setRedirectUris(redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        entity.addRedirectUri(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        entity.removeRedirectUri(redirectUri);
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
        final Boolean bearerOnly = entity.isBearerOnly();
        return bearerOnly == null ? false : bearerOnly;
    }

    @Override
    public void setBearerOnly(boolean only) {
        entity.setBearerOnly(only);
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
    public boolean validateSecret(String secret) {
        return MessageDigest.isEqual(secret.getBytes(), entity.getSecret().getBytes());
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
    public int getNodeReRegistrationTimeout() {
        final Integer nodeReRegistrationTimeout = entity.getNodeReRegistrationTimeout();
        return nodeReRegistrationTimeout == null ? 0 : nodeReRegistrationTimeout;
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        entity.setNodeReRegistrationTimeout(timeout);
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
    public String getProtocol() {
        return entity.getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        if (!Objects.equals(entity.getProtocol(), protocol)) {
            entity.setProtocol(protocol);
            session.getKeycloakSessionFactory().publish((ClientModel.ClientProtocolUpdatedEvent) () -> MapClientAdapter.this);
        }
    }

    @Override
    public void setAttribute(String name, String value) {
        boolean valueUndefined = value == null || "".equals(value.trim());

        if (valueUndefined) {
            removeAttribute(name);
            return;
        }

        entity.setAttribute(name, Collections.singletonList(value));
    }

    @Override
    public void removeAttribute(String name) {
        entity.removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        List<String> attribute = entity.getAttribute(name);
        if (attribute == null || attribute.isEmpty()) return null;
        return attribute.get(0);
    }

    @Override
    public Map<String, String> getAttributes() {
        final Map<String, List<String>> attributes = entity.getAttributes();
        final Map<String, List<String>> a = attributes == null ? Collections.emptyMap() : attributes;
        return a.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            entry -> {
                if (entry.getValue().isEmpty()) {
                    return null;
                } else if (entry.getValue().size() > 1) {
                    // This could be caused by an inconsistency in the storage, a programming error,
                    // or a downgrade from a future version of Keycloak that already supports multi-valued attributes.
                    // The caller will not see the other values, and when this entity is later updated, the additional values will be lost.
                    LOG.warnf("Client '%s' realm '%s' has attribute '%s' with %d values, retrieving only the first", getClientId(), getRealm().getName(), entry.getKey(),
                            entry.getValue().size());
                }
                return entry.getValue().get(0);
            })
        );
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return entity.getAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        final Map<String, String> authenticationFlowBindingOverrides = entity.getAuthenticationFlowBindingOverrides();
        return authenticationFlowBindingOverrides == null ? Collections.emptyMap() : authenticationFlowBindingOverrides;
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        entity.removeAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        entity.setAuthenticationFlowBindingOverride(binding, flowId);
    }

    @Override
    public boolean isFrontchannelLogout() {
        final Boolean frontchannelLogout = entity.isFrontchannelLogout();
        return frontchannelLogout == null ? false : frontchannelLogout;
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        entity.setFrontchannelLogout(flag);
    }

    @Override
    public boolean isFullScopeAllowed() {
        final Boolean fullScopeAllowed = entity.isFullScopeAllowed();
        return fullScopeAllowed == null ? false : fullScopeAllowed;
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        entity.setFullScopeAllowed(value);
    }

    @Override
    public boolean isPublicClient() {
        final Boolean publicClient = entity.isPublicClient();
        return publicClient == null ? false : publicClient;
    }

    @Override
    public void setPublicClient(boolean flag) {
        entity.setPublicClient(flag);
    }

    @Override
    public boolean isConsentRequired() {
        final Boolean consentRequired = entity.isConsentRequired();
        return consentRequired == null ? false : consentRequired;
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        entity.setConsentRequired(consentRequired);
    }

    @Override
    public boolean isStandardFlowEnabled() {
        final Boolean standardFlowEnabled = entity.isStandardFlowEnabled();
        return standardFlowEnabled == null ? false : standardFlowEnabled;
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        entity.setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        final Boolean implicitFlowEnabled = entity.isImplicitFlowEnabled();
        return implicitFlowEnabled == null ? false : implicitFlowEnabled;
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        entity.setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        final Boolean directAccessGrantsEnabled = entity.isDirectAccessGrantsEnabled();
        return directAccessGrantsEnabled == null ? false : directAccessGrantsEnabled;
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        entity.setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        final Boolean serviceAccountsEnabled = entity.isServiceAccountsEnabled();
        return serviceAccountsEnabled == null ? false : serviceAccountsEnabled;
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        entity.setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public int getNotBefore() {
        final Integer notBefore = entity.getNotBefore();
        return notBefore == null ? 0 : notBefore;
    }

    @Override
    public void setNotBefore(int notBefore) {
        entity.setNotBefore(notBefore);
    }

    /*************** Scopes mappings ****************/

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        final Collection<String> scopeMappings = this.entity.getScopeMappings();
        return scopeMappings == null ? Stream.empty() : scopeMappings.stream()
                .map(realm::getRoleById)
                .filter(Objects::nonNull);
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        final String id = role == null ? null : role.getId();
        if (id != null) {
            this.entity.addScopeMapping(id);
        }
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        final String id = role == null ? null : role.getId();
        if (id != null) {
            this.entity.removeScopeMapping(id);
        }
    }

    @Override
    public boolean hasDirectScope(RoleModel role) {
        final String id = role == null ? null : role.getId();
        if (id != null && this.entity.getScopeMappings().contains(id)) {
            return true;
        }

        return getRolesStream().anyMatch(r -> (Objects.equals(r, role)));
    }

    @Override
    public boolean hasScope(RoleModel role) {
        if (isFullScopeAllowed()) return true;

        final String id = role == null ? null : role.getId();
        final Collection<String> scopeMappings = this.entity.getScopeMappings();
        if (id != null && scopeMappings != null && scopeMappings.contains(id)) {
            return true;
        }

        if (getScopeMappingsStream().anyMatch(r -> r.hasRole(role))) {
            return true;
        }

        return getRolesStream().anyMatch(r -> (Objects.equals(r, role) || r.hasRole(role)));
    }

    /*************** Default roles ****************/

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

    /*************** Protocol mappers ****************/

    private String safeGetProtocol() {
        return entity.getProtocol() == null ? "openid-connect" : entity.getProtocol();
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        final Map<String, MapProtocolMapperEntity> protocolMappers = entity.getProtocolMappers();
        return protocolMappers == null ? Stream.empty() : protocolMappers.values().stream().distinct()
          .map(pmUtils::toModel);
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        if (model == null) {
            return null;
        }

        MapProtocolMapperEntity pm = MapProtocolMapperUtils.fromModel(model);
        if (pm.getId() == null) {
            String id = KeycloakModelUtils.generateId();
            pm.setId(id);
        }
        if (model.getConfig() == null) {
            pm.setConfig(new HashMap<>());
        }

        entity.setProtocolMapper(pm.getId(), pm);
        return pmUtils.toModel(pm);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        final String id = mapping == null ? null : mapping.getId();
        if (id != null) {
            entity.removeProtocolMapper(id);
        }
    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        final String id = mapping == null ? null : mapping.getId();
        if (id != null) {
            entity.setProtocolMapper(id, MapProtocolMapperUtils.fromModel(mapping));
        }
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        MapProtocolMapperEntity protocolMapper = entity.getProtocolMapper(id);
        return protocolMapper == null ? null : pmUtils.toModel(protocolMapper);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        final Map<String, MapProtocolMapperEntity> protocolMappers = entity.getProtocolMappers();
        if (! Objects.equals(protocol, safeGetProtocol())) {
            return null;
        }
        return protocolMappers == null ? null : protocolMappers.values().stream()
          .filter(pm -> Objects.equals(pm.getName(), name))
          .map(pmUtils::toModel)
          .findAny()
          .orElse(null);
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getClientId(), System.identityHashCode(this));
    }
}
