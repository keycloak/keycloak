/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public class MapClientEntityLazyDelegate<K> implements MapClientEntity<K> {

    private final Supplier<MapClientEntity<K>> delegateSupplier;

    private final AtomicMarkableReference<MapClientEntity<K>> delegate = new AtomicMarkableReference<>(null, false);

    public MapClientEntityLazyDelegate(Supplier<MapClientEntity<K>> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    protected MapClientEntity<K> getDelegate() {
        if (! delegate.isMarked()) {
            delegate.compareAndSet(null, delegateSupplier == null ? null : delegateSupplier.get(), false, true);
        }
        MapClientEntity<K> ref = delegate.getReference();
        if (ref == null) {
            throw new IllegalStateException("Invalid delegate obtained");
        }
        return ref;
    }

    @Override
    public void addClientScope(String id, Boolean defaultScope) {
        getDelegate().addClientScope(id, defaultScope);
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        return getDelegate().addProtocolMapper(model);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getDelegate().addRedirectUri(redirectUri);
    }

    @Override
    public void addScopeMapping(String id) {
        getDelegate().addScopeMapping(id);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getDelegate().addWebOrigin(webOrigin);
    }

    @Override
    public void deleteScopeMapping(String id) {
        getDelegate().deleteScopeMapping(id);
    }

    @Override
    public List<String> getAttribute(String name) {
        return getDelegate().getAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return getDelegate().getAttributes();
    }

    @Override
    public Map<String, String> getAuthFlowBindings() {
        return getDelegate().getAuthFlowBindings();
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return getDelegate().getAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return getDelegate().getAuthenticationFlowBindingOverrides();
    }

    @Override
    public String getBaseUrl() {
        return getDelegate().getBaseUrl();
    }

    @Override
    public String getClientAuthenticatorType() {
        return getDelegate().getClientAuthenticatorType();
    }

    @Override
    public String getClientId() {
        return getDelegate().getClientId();
    }

    @Override
    public Stream<String> getClientScopes(boolean defaultScope) {
        return getDelegate().getClientScopes(defaultScope);
    }

    @Override
    public String getDescription() {
        return getDelegate().getDescription();
    }

    @Override
    public String getManagementUrl() {
        return getDelegate().getManagementUrl();
    }

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return getDelegate().getNodeReRegistrationTimeout();
    }

    @Override
    public int getNotBefore() {
        return getDelegate().getNotBefore();
    }

    @Override
    public String getProtocol() {
        return getDelegate().getProtocol();
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return getDelegate().getProtocolMapperById(id);
    }

    @Override
    public Collection<ProtocolMapperModel> getProtocolMappers() {
        return getDelegate().getProtocolMappers();
    }

    @Override
    public String getRealmId() {
        return getDelegate().getRealmId();
    }

    @Override
    public Set<String> getRedirectUris() {
        return getDelegate().getRedirectUris();
    }

    @Override
    public String getRegistrationToken() {
        return getDelegate().getRegistrationToken();
    }

    @Override
    public String getRootUrl() {
        return getDelegate().getRootUrl();
    }

    @Override
    public Set<String> getScope() {
        return getDelegate().getScope();
    }

    @Override
    public Collection<String> getScopeMappings() {
        return getDelegate().getScopeMappings();
    }

    @Override
    public String getSecret() {
        return getDelegate().getSecret();
    }

    @Override
    public Set<String> getWebOrigins() {
        return getDelegate().getWebOrigins();
    }

    @Override
    public Boolean isAlwaysDisplayInConsole() {
        return getDelegate().isAlwaysDisplayInConsole();
    }

    @Override
    public Boolean isBearerOnly() {
        return getDelegate().isBearerOnly();
    }

    @Override
    public Boolean isConsentRequired() {
        return getDelegate().isConsentRequired();
    }

    @Override
    public Boolean isDirectAccessGrantsEnabled() {
        return getDelegate().isDirectAccessGrantsEnabled();
    }

    @Override
    public Boolean isEnabled() {
        return getDelegate().isEnabled();
    }

    @Override
    public Boolean isFrontchannelLogout() {
        return getDelegate().isFrontchannelLogout();
    }

    @Override
    public Boolean isFullScopeAllowed() {
        return getDelegate().isFullScopeAllowed();
    }

    @Override
    public Boolean isImplicitFlowEnabled() {
        return getDelegate().isImplicitFlowEnabled();
    }

    @Override
    public Boolean isPublicClient() {
        return getDelegate().isPublicClient();
    }

    @Override
    public Boolean isServiceAccountsEnabled() {
        return getDelegate().isServiceAccountsEnabled();
    }

    @Override
    public Boolean isStandardFlowEnabled() {
        return getDelegate().isStandardFlowEnabled();
    }

    @Override
    public Boolean isSurrogateAuthRequired() {
        return getDelegate().isSurrogateAuthRequired();
    }

    @Override
    public void removeAttribute(String name) {
        getDelegate().removeAttribute(name);
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        getDelegate().removeAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public void removeClientScope(String id) {
        getDelegate().removeClientScope(id);
    }

    @Override
    public void removeProtocolMapper(String id) {
        getDelegate().removeProtocolMapper(id);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getDelegate().removeRedirectUri(redirectUri);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getDelegate().removeWebOrigin(webOrigin);
    }

    @Override
    public void setAlwaysDisplayInConsole(Boolean alwaysDisplayInConsole) {
        getDelegate().setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getDelegate().setAttribute(name, values);
    }

    @Override
    public void setAuthFlowBindings(Map<String, String> authFlowBindings) {
        getDelegate().setAuthFlowBindings(authFlowBindings);
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        getDelegate().setAuthenticationFlowBindingOverride(binding, flowId);
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        getDelegate().setBaseUrl(baseUrl);
    }

    @Override
    public void setBearerOnly(Boolean bearerOnly) {
        getDelegate().setBearerOnly(bearerOnly);
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        getDelegate().setClientAuthenticatorType(clientAuthenticatorType);
    }

    @Override
    public void setClientId(String clientId) {
        getDelegate().setClientId(clientId);
    }

    @Override
    public void setConsentRequired(Boolean consentRequired) {
        getDelegate().setConsentRequired(consentRequired);
    }

    @Override
    public void setDescription(String description) {
        getDelegate().setDescription(description);
    }

    @Override
    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        getDelegate().setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public void setEnabled(Boolean enabled) {
        getDelegate().setEnabled(enabled);
    }

    @Override
    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        getDelegate().setFrontchannelLogout(frontchannelLogout);
    }

    @Override
    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        getDelegate().setFullScopeAllowed(fullScopeAllowed);
    }

    @Override
    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        getDelegate().setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public void setManagementUrl(String managementUrl) {
        getDelegate().setManagementUrl(managementUrl);
    }

    @Override
    public void setName(String name) {
        getDelegate().setName(name);
    }

    @Override
    public void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout) {
        getDelegate().setNodeReRegistrationTimeout(nodeReRegistrationTimeout);
    }

    @Override
    public void setNotBefore(int notBefore) {
        getDelegate().setNotBefore(notBefore);
    }

    @Override
    public void setProtocol(String protocol) {
        getDelegate().setProtocol(protocol);
    }

    @Override
    public void setProtocolMappers(Collection<ProtocolMapperModel> protocolMappers) {
        getDelegate().setProtocolMappers(protocolMappers);
    }

    @Override
    public void setPublicClient(Boolean publicClient) {
        getDelegate().setPublicClient(publicClient);
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        getDelegate().setRedirectUris(redirectUris);
    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        getDelegate().setRegistrationToken(registrationToken);
    }

    @Override
    public void setRootUrl(String rootUrl) {
        getDelegate().setRootUrl(rootUrl);
    }

    @Override
    public void setScope(Set<String> scope) {
        getDelegate().setScope(scope);
    }

    @Override
    public void setSecret(String secret) {
        getDelegate().setSecret(secret);
    }

    @Override
    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        getDelegate().setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        getDelegate().setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public void setSurrogateAuthRequired(Boolean surrogateAuthRequired) {
        getDelegate().setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        getDelegate().setWebOrigins(webOrigins);
    }

    @Override
    public void updateProtocolMapper(String id, ProtocolMapperModel mapping) {
        getDelegate().updateProtocolMapper(id, mapping);
    }

    @Override
    public K getId() {
        return getDelegate().getId();
    }

    @Override
    public boolean isUpdated() {
        return getDelegate().isUpdated();
    }

}
