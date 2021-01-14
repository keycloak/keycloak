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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.cache.infinispan.DefaultLazyLoader;
import org.keycloak.models.cache.infinispan.LazyLoader;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedClient extends AbstractRevisioned implements InRealm {

    private static Set<ProtocolMapperModel> fetchMappers(ClientModel c) {
        return c.getProtocolMappersStream().collect(Collectors.toSet());
    }

    private static Map<String, Integer> fetchRegisteredNodes(ClientModel c) {
        return new TreeMap<>(c.getRegisteredNodes());
    }

    private static List<String> fetchDefaultClientScopes(ClientModel c) {
        return c.getClientScopes(true, false).values().stream().map(ClientScopeModel::getId)
                .collect(Collectors.toList());
    }

    private static List<String> fetchOptionalClientScopes(ClientModel c) {
        return c.getClientScopes(false, false).values().stream().map(ClientScopeModel::getId)
                .collect(Collectors.toList());
    }

    protected String clientId;
    protected String name;
    protected String description;
    protected String realm;
    protected LazyLoader<ClientModel, Set<String>> redirectUris;
    protected boolean enabled;
    protected boolean alwaysDisplayInConsole;
    protected String clientAuthenticatorType;
    protected String secret;
    protected String registrationToken;
    protected String protocol;
    protected LazyLoader<ClientModel, Map<String, String>> attributes;
    protected LazyLoader<ClientModel, Map<String, String>> authFlowBindings;
    protected boolean publicClient;
    protected boolean fullScopeAllowed;
    protected boolean frontchannelLogout;
    protected int notBefore;
    protected Set<String> scope = new HashSet<>();
    protected LazyLoader<ClientModel, Set<String>> webOrigins;
    protected LazyLoader<ClientModel, Set<ProtocolMapperModel>> protocolMappers;
    protected boolean surrogateAuthRequired;
    protected String managementUrl;
    protected String rootUrl;
    protected String baseUrl;
    protected boolean bearerOnly;
    protected boolean consentRequired;
    protected boolean standardFlowEnabled;
    protected boolean implicitFlowEnabled;
    protected boolean directAccessGrantsEnabled;
    protected boolean serviceAccountsEnabled;
    protected int nodeReRegistrationTimeout;
    protected LazyLoader<ClientModel, Map<String, Integer>> registeredNodes;
    protected LazyLoader<ClientModel, List<String>> defaultClientScopesIds;
    protected LazyLoader<ClientModel, List<String>> optionalClientScopesIds;

    public CachedClient(Long revision, RealmModel realm, ClientModel model) {
        super(revision, model.getId());
        clientAuthenticatorType = model.getClientAuthenticatorType();
        secret = model.getSecret();
        registrationToken = model.getRegistrationToken();
        clientId = model.getClientId();
        name = model.getName();
        description = model.getDescription();
        this.realm = realm.getId();
        enabled = model.isEnabled();
        alwaysDisplayInConsole = model.isAlwaysDisplayInConsole();
        protocol = model.getProtocol();
        attributes = new DefaultLazyLoader<>(ClientModel::getAttributes, Collections::emptyMap);
        authFlowBindings = new DefaultLazyLoader<>(ClientModel::getAuthenticationFlowBindingOverrides, Collections::emptyMap);
        notBefore = model.getNotBefore();
        frontchannelLogout = model.isFrontchannelLogout();
        publicClient = model.isPublicClient();
        fullScopeAllowed = model.isFullScopeAllowed();
        redirectUris = new DefaultLazyLoader<>(ClientModel::getRedirectUris, Collections::emptySet);
        webOrigins = new DefaultLazyLoader<>(ClientModel::getWebOrigins, Collections::emptySet);
        scope.addAll(model.getScopeMappingsStream().map(RoleModel::getId).collect(Collectors.toSet()));
        protocolMappers = new DefaultLazyLoader<>(CachedClient::fetchMappers, Collections::emptySet);
        surrogateAuthRequired = model.isSurrogateAuthRequired();
        managementUrl = model.getManagementUrl();
        rootUrl = model.getRootUrl();
        baseUrl = model.getBaseUrl();
        bearerOnly = model.isBearerOnly();
        consentRequired = model.isConsentRequired();
        standardFlowEnabled = model.isStandardFlowEnabled();
        implicitFlowEnabled = model.isImplicitFlowEnabled();
        directAccessGrantsEnabled = model.isDirectAccessGrantsEnabled();
        serviceAccountsEnabled = model.isServiceAccountsEnabled();

        nodeReRegistrationTimeout = model.getNodeReRegistrationTimeout();
        registeredNodes = new DefaultLazyLoader<>(CachedClient::fetchRegisteredNodes, Collections::emptyMap);

        defaultClientScopesIds = new DefaultLazyLoader<>(CachedClient::fetchDefaultClientScopes, Collections::emptyList);
        optionalClientScopesIds = new DefaultLazyLoader<>(CachedClient::fetchOptionalClientScopes, Collections::emptyList);
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getRealm() {
        return realm;
    }

    public Set<String> getRedirectUris(Supplier<ClientModel> client) {
        return redirectUris.get(client);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAlwaysDisplayInConsole() {
        return alwaysDisplayInConsole;
    }

    public String getClientAuthenticatorType() {
        return clientAuthenticatorType;
    }

    public String getSecret() {
        return secret;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public boolean isPublicClient() {
        return publicClient;
    }

    public int getNotBefore() {
        return notBefore;
    }

    public Set<String> getScope() {
        return scope;
    }

    public Set<String> getWebOrigins(Supplier<ClientModel> client) {
        return webOrigins.get(client);
    }

    public boolean isFullScopeAllowed() {
        return fullScopeAllowed;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map<String, String> getAttributes(Supplier<ClientModel> client) {
        return attributes.get(client);
    }

    public boolean isFrontchannelLogout() {
        return frontchannelLogout;
    }

    public Set<ProtocolMapperModel> getProtocolMappers(Supplier<ClientModel> client) {
        return protocolMappers.get(client);
    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public String getRootUrl() {
        return rootUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public boolean isStandardFlowEnabled() {
        return standardFlowEnabled;
    }

    public boolean isImplicitFlowEnabled() {
        return implicitFlowEnabled;
    }

    public boolean isDirectAccessGrantsEnabled() {
        return directAccessGrantsEnabled;
    }

    public boolean isServiceAccountsEnabled() {
        return serviceAccountsEnabled;
    }

    public int getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    public Map<String, Integer> getRegisteredNodes(Supplier<ClientModel> client) {
        return registeredNodes.get(client);
    }

    public List<String> getDefaultClientScopesIds(Supplier<ClientModel> client) {
        return defaultClientScopesIds.get(client);
    }

    public List<String> getOptionalClientScopesIds(Supplier<ClientModel> client) {
        return optionalClientScopesIds.get(client);
    }

    public Map<String, String> getAuthFlowBindings(Supplier<ClientModel> client) {
        return authFlowBindings.get(client);
    }
}
