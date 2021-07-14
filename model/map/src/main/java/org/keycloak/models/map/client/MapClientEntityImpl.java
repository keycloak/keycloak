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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
public class MapClientEntityImpl<K> implements MapClientEntity<K> {

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
    private Map<String, List<String>> attributes = new HashMap<>();
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

    protected MapClientEntityImpl() {
        this.id = null;
        this.realmId = null;
    }

    public MapClientEntityImpl(K id, String realmId) {
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

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(String clientId) {
        this.updated |= ! Objects.equals(this.clientId, clientId);
        this.clientId = clientId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.updated |= ! Objects.equals(this.name, name);
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.updated |= ! Objects.equals(this.description, description);
        this.description = description;
    }

    @Override
    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        this.updated |= ! Objects.equals(this.redirectUris, redirectUris);
        this.redirectUris = redirectUris;
    }

    @Override
    public Boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(Boolean enabled) {
        this.updated |= ! Objects.equals(this.enabled, enabled);
        this.enabled = enabled;
    }

    @Override
    public Boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole;
    }

    @Override
    public void setAlwaysDisplayInConsole(Boolean alwaysDisplayInConsole) {
        this.updated |= ! Objects.equals(this.alwaysDisplayInConsole, alwaysDisplayInConsole);
        this.alwaysDisplayInConsole = alwaysDisplayInConsole;
    }

    @Override
    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        this.updated |= ! Objects.equals(this.clientAuthenticatorType, clientAuthenticatorType);
        this.clientAuthenticatorType = clientAuthenticatorType;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public void setSecret(String secret) {
        this.updated |= ! Objects.equals(this.secret, secret);
        this.secret = secret;
    }

    @Override
    public String getRegistrationToken() {
        return registrationToken;
    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        this.updated |= ! Objects.equals(this.registrationToken, registrationToken);
        this.registrationToken = registrationToken;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        this.updated |= ! Objects.equals(this.protocol, protocol);
        this.protocol = protocol;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes;
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        this.updated |= ! Objects.equals(this.attributes.put(name, values), values);
    }

    @Override
    public Map<String, String> getAuthFlowBindings() {
        return authFlowBindings;
    }

    @Override
    public void setAuthFlowBindings(Map<String, String> authFlowBindings) {
        this.updated |= ! Objects.equals(this.authFlowBindings, authFlowBindings);
        this.authFlowBindings = authFlowBindings;
    }

    @Override
    public Boolean isPublicClient() {
        return publicClient;
    }

    @Override
    public void setPublicClient(Boolean publicClient) {
        this.updated |= ! Objects.equals(this.publicClient, publicClient);
        this.publicClient = publicClient;
    }

    @Override
    public Boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    @Override
    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        this.updated |= ! Objects.equals(this.fullScopeAllowed, fullScopeAllowed);
        this.fullScopeAllowed = fullScopeAllowed;
    }

    @Override
    public Boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    @Override
    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        this.updated |= ! Objects.equals(this.frontchannelLogout, frontchannelLogout);
        this.frontchannelLogout = frontchannelLogout;
    }

    @Override
    public int getNotBefore() {
        return notBefore;
    }

    @Override
    public void setNotBefore(int notBefore) {
        this.updated |= ! Objects.equals(this.notBefore, notBefore);
        this.notBefore = notBefore;
    }

    @Override
    public Set<String> getScope() {
        return scope;
    }

    @Override
    public void setScope(Set<String> scope) {
        this.updated |= ! Objects.equals(this.scope, scope);
        this.scope.clear();
        this.scope.addAll(scope);
    }

    @Override
    public Set<String> getWebOrigins() {
        return webOrigins;
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        this.updated |= ! Objects.equals(this.webOrigins, webOrigins);
        this.webOrigins.clear();
        this.webOrigins.addAll(webOrigins);
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        Objects.requireNonNull(model.getId(), "protocolMapper.id");
        updated = true;
        this.protocolMappers.put(model.getId(), model);
        return model;
    }

    @Override
    public Collection<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers.values();
    }

    @Override
    public void updateProtocolMapper(String id, ProtocolMapperModel mapping) {
        updated = true;
        protocolMappers.put(id, mapping);
    }

    @Override
    public void removeProtocolMapper(String id) {
        updated |= protocolMappers.remove(id) != null;
    }

    @Override
    public void setProtocolMappers(Collection<ProtocolMapperModel> protocolMappers) {
        this.updated |= ! Objects.equals(this.protocolMappers, protocolMappers);
        this.protocolMappers.clear();
        this.protocolMappers.putAll(protocolMappers.stream().collect(Collectors.toMap(ProtocolMapperModel::getId, Function.identity())));
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return id == null ? null : protocolMappers.get(id);
    }

    @Override
    public Boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    @Override
    public void setSurrogateAuthRequired(Boolean surrogateAuthRequired) {
        this.updated |= ! Objects.equals(this.surrogateAuthRequired, surrogateAuthRequired);
        this.surrogateAuthRequired = surrogateAuthRequired;
    }

    @Override
    public String getManagementUrl() {
        return managementUrl;
    }

    @Override
    public void setManagementUrl(String managementUrl) {
        this.updated |= ! Objects.equals(this.managementUrl, managementUrl);
        this.managementUrl = managementUrl;
    }

    @Override
    public String getRootUrl() {
        return rootUrl;
    }

    @Override
    public void setRootUrl(String rootUrl) {
        this.updated |= ! Objects.equals(this.rootUrl, rootUrl);
        this.rootUrl = rootUrl;
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        this.updated |= ! Objects.equals(this.baseUrl, baseUrl);
        this.baseUrl = baseUrl;
    }

    @Override
    public Boolean isBearerOnly() {
        return bearerOnly;
    }

    @Override
    public void setBearerOnly(Boolean bearerOnly) {
        this.updated |= ! Objects.equals(this.bearerOnly, bearerOnly);
        this.bearerOnly = bearerOnly;
    }

    @Override
    public Boolean isConsentRequired() {
        return consentRequired;
    }

    @Override
    public void setConsentRequired(Boolean consentRequired) {
        this.updated |= ! Objects.equals(this.consentRequired, consentRequired);
        this.consentRequired = consentRequired;
    }

    @Override
    public Boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    @Override
    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        this.updated |= ! Objects.equals(this.standardFlowEnabled, standardFlowEnabled);
        this.standardFlowEnabled = standardFlowEnabled;
    }

    @Override
    public Boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    @Override
    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        this.updated |= ! Objects.equals(this.implicitFlowEnabled, implicitFlowEnabled);
        this.implicitFlowEnabled = implicitFlowEnabled;
    }

    @Override
    public Boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    @Override
    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        this.updated |= ! Objects.equals(this.directAccessGrantsEnabled, directAccessGrantsEnabled);
        this.directAccessGrantsEnabled = directAccessGrantsEnabled;
    }

    @Override
    public Boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    @Override
    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        this.updated |= ! Objects.equals(this.serviceAccountsEnabled, serviceAccountsEnabled);
        this.serviceAccountsEnabled = serviceAccountsEnabled;
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    @Override
    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        this.updated |= ! Objects.equals(this.nodeReRegistrationTimeout, nodeReRegistrationTimeout);
        this.nodeReRegistrationTimeout = nodeReRegistrationTimeout;
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        updated = true;
        this.webOrigins.add(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        updated |= this.webOrigins.remove(webOrigin);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        this.updated |= ! this.redirectUris.contains(redirectUri);
        this.redirectUris.add(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        updated |= this.redirectUris.remove(redirectUri);
    }

    @Override
    public void removeAttribute(String name) {
        this.updated |= this.attributes.remove(name) != null;
    }

    @Override
    public List<String> getAttribute(String name) {
        return attributes.getOrDefault(name, Collections.EMPTY_LIST);
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return this.authFlowBindings.get(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return this.authFlowBindings;
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        updated |= this.authFlowBindings.remove(binding) != null;
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        this.updated = true;
        this.authFlowBindings.put(binding, flowId);
    }

    @Override
    public Collection<String> getScopeMappings() {
        return scopeMappings;
    }

    @Override
    public void addScopeMapping(String id) {
        if (id != null) {
            updated = true;
            scopeMappings.add(id);
        }
    }

    @Override
    public void deleteScopeMapping(String id) {
        updated |= scopeMappings.remove(id);
    }

    @Override
    public void addClientScope(String id, Boolean defaultScope) {
        if (id != null) {
            updated = true;
            this.clientScopes.put(id, defaultScope);
        }
    }

    @Override
    public void removeClientScope(String id) {
        if (id != null) {
            updated |= clientScopes.remove(id) != null;
        }
    }

    @Override
    public Stream<String> getClientScopes(boolean defaultScope) {
        return this.clientScopes.entrySet().stream()
          .filter(me -> Objects.equals(me.getValue(), defaultScope))
          .map(Entry::getKey);
    }

    @Override
    public String getRealmId() {
        return this.realmId;
    }

}
