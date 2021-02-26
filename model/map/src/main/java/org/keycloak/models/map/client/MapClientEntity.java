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

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.map.common.AbstractEntity;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class MapClientEntity<K> implements AbstractEntity<K> {

    private K id;
    private String realmId;

    private String clientId;
    private String name;
    private String description;
    private Set<String> redirectUris = new HashSet<>();
    private boolean enabled;
    private boolean alwaysDisplayInConsole;
    private String clientAuthenticatorType;
    private String secret;
    private String registrationToken;
    private String protocol;
    private Map<String, String> attributes = new HashMap<>();
    private Map<String, String> authFlowBindings = new HashMap<>();
    private boolean publicClient;
    private boolean fullScopeAllowed;
    private boolean frontchannelLogout;
    private int notBefore;
    private Set<String> scope = new HashSet<>();
    private Set<String> webOrigins = new HashSet<>();
    private Map<String, ProtocolMapperModel> protocolMappers = new HashMap<>();
    private Map<String, Boolean> clientScopes = new HashMap<>();
    private Set<String> scopeMappings = new LinkedHashSet<>();
    private boolean surrogateAuthRequired;
    private String managementUrl;
    private String rootUrl;
    private String baseUrl;
    private boolean bearerOnly;
    private boolean consentRequired;
    private boolean standardFlowEnabled;
    private boolean implicitFlowEnabled;
    private boolean directAccessGrantsEnabled;
    private boolean serviceAccountsEnabled;
    private int nodeReRegistrationTimeout;

    /**
     * Flag signalizing that any of the setters has been meaningfully used.
     */
    protected boolean updated;

    protected MapClientEntity() {
        this.id = null;
        this.realmId = null;
    }

    public MapClientEntity(K id, String realmId) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(realmId, "realmId");

        this.id = id;
        this.realmId = realmId;
    }

    @Override
    public K getId() {
        return this.id;
    }

    @Override
    public boolean isUpdated() {
        return this.updated;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.updated |= ! Objects.equals(this.clientId, clientId);
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated |= ! Objects.equals(this.name, name);
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.updated |= ! Objects.equals(this.description, description);
        this.description = description;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.updated |= ! Objects.equals(this.redirectUris, redirectUris);
        this.redirectUris = redirectUris;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.updated |= ! Objects.equals(this.enabled, enabled);
        this.enabled = enabled;
    }

    public boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole;
    }

    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        this.updated |= ! Objects.equals(this.alwaysDisplayInConsole, alwaysDisplayInConsole);
        this.alwaysDisplayInConsole = alwaysDisplayInConsole;
    }

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        this.updated |= ! Objects.equals(this.clientAuthenticatorType, clientAuthenticatorType);
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.updated |= ! Objects.equals(this.secret, secret);
        this.secret = secret;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public void setRegistrationToken(String registrationToken) {
        this.updated |= ! Objects.equals(this.registrationToken, registrationToken);
        this.registrationToken = registrationToken;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.updated |= ! Objects.equals(this.protocol, protocol);
        this.protocol = protocol;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.updated |= ! Objects.equals(this.attributes, attributes);
        this.attributes = attributes;
    }

    public Map<String, String> getAuthFlowBindings() {
        return authFlowBindings;
    }

    public void setAuthFlowBindings(Map<String, String> authFlowBindings) {
        this.updated |= ! Objects.equals(this.authFlowBindings, authFlowBindings);
        this.authFlowBindings = authFlowBindings;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public void setPublicClient(boolean publicClient) {
        this.updated |= ! Objects.equals(this.publicClient, publicClient);
        this.publicClient = publicClient;
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public void setFullScopeAllowed(boolean fullScopeAllowed) {
        this.updated |= ! Objects.equals(this.fullScopeAllowed, fullScopeAllowed);
        this.fullScopeAllowed = fullScopeAllowed;
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public void setFrontchannelLogout(boolean frontchannelLogout) {
        this.updated |= ! Objects.equals(this.frontchannelLogout, frontchannelLogout);
        this.frontchannelLogout = frontchannelLogout;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(int notBefore) {
        this.updated |= ! Objects.equals(this.notBefore, notBefore);
        this.notBefore = notBefore;
    }

    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.updated |= ! Objects.equals(this.scope, scope);
        this.scope.clear();
        this.scope.addAll(scope);
    }

    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    public void setWebOrigins(Set<String> webOrigins) {
        this.updated |= ! Objects.equals(this.webOrigins, webOrigins);
        this.webOrigins.clear();
        this.webOrigins.addAll(webOrigins);
    }

    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        Objects.requireNonNull(model.getId(), "protocolMapper.id");
        updated = true;
        this.protocolMappers.put(model.getId(), model);
        return model;
    }

    public Collection<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers.values();
    }

    public void updateProtocolMapper(String id, ProtocolMapperModel mapping) {
        updated = true;
        protocolMappers.put(id, mapping);
    }

    public void removeProtocolMapper(String id) {
        updated |= protocolMappers.remove(id) != null;
    }

    public void setProtocolMappers(Collection<ProtocolMapperModel> protocolMappers) {
        this.updated |= ! Objects.equals(this.protocolMappers, protocolMappers);
        this.protocolMappers.clear();
        this.protocolMappers.putAll(protocolMappers.stream().collect(Collectors.toMap(ProtocolMapperModel::getId, Function.identity())));
    }

    public ProtocolMapperModel getProtocolMapperById(String id) {
        return id == null ? null : protocolMappers.get(id);
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        this.updated |= ! Objects.equals(this.surrogateAuthRequired, surrogateAuthRequired);
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public void setManagementUrl(String managementUrl) {
        this.updated |= ! Objects.equals(this.managementUrl, managementUrl);
        this.managementUrl = managementUrl;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.updated |= ! Objects.equals(this.rootUrl, rootUrl);
        this.rootUrl = rootUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.updated |= ! Objects.equals(this.baseUrl, baseUrl);
        this.baseUrl = baseUrl;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public void setBearerOnly(boolean bearerOnly) {
        this.updated |= ! Objects.equals(this.bearerOnly, bearerOnly);
        this.bearerOnly = bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.updated |= ! Objects.equals(this.consentRequired, consentRequired);
        this.consentRequired = consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        this.updated |= ! Objects.equals(this.standardFlowEnabled, standardFlowEnabled);
        this.standardFlowEnabled = standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        this.updated |= ! Objects.equals(this.implicitFlowEnabled, implicitFlowEnabled);
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        this.updated |= ! Objects.equals(this.directAccessGrantsEnabled, directAccessGrantsEnabled);
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        this.updated |= ! Objects.equals(this.serviceAccountsEnabled, serviceAccountsEnabled);
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.updated |= ! Objects.equals(this.nodeReRegistrationTimeout, nodeReRegistrationTimeout);
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    public void addWebOrigin(String webOrigin) {
        updated = true;
        this.webOrigins.add(webOrigin);
    }

    public void removeWebOrigin(String webOrigin) {
        updated |= this.webOrigins.remove(webOrigin);
    }

    public void addRedirectUri(String redirectUri) {
        this.updated |= ! this.redirectUris.contains(redirectUri);
        this.redirectUris.add(redirectUri);
    }

    public void removeRedirectUri(String redirectUri) {
        updated |= this.redirectUris.remove(redirectUri);
    }

    public void setAttribute(String name, String value) {
        this.updated = true;
        this.attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        this.updated |= this.attributes.remove(name) != null;
    }

    public String getAttribute(String name) {
        return this.attributes.get(name);
    }

    public String getAuthenticationFlowBindingOverride(String binding) {
        return this.authFlowBindings.get(binding);
    }

    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return this.authFlowBindings;
    }

    public void removeAuthenticationFlowBindingOverride(String binding) {
        updated |= this.authFlowBindings.remove(binding) != null;
    }

    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        this.updated = true;
        this.authFlowBindings.put(binding, flowId);
    }

    public Collection<String> getScopeMappings() {
        return scopeMappings;
    }

    public void addScopeMapping(String id) {
        if (id != null) {
            updated = true;
            scopeMappings.add(id);
        }
    }

    public void deleteScopeMapping(String id) {
        updated |= scopeMappings.remove(id);
    }

    public void addClientScope(String id, boolean defaultScope) {
        if (id != null) {
            updated = true;
            this.clientScopes.put(id, defaultScope);
        }
    }

    public void removeClientScope(String id) {
        if (id != null) {
            updated |= clientScopes.remove(id) != null;
        }
    }

    public Stream<String> getClientScopes(boolean defaultScope) {
        return this.clientScopes.entrySet().stream()
          .filter(me -> Objects.equals(me.getValue(), defaultScope))
          .map(Entry::getKey);
    }

    public String getRealmId() {
        return this.realmId;
    }

}
