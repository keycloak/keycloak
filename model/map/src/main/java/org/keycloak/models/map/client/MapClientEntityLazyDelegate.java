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
public class MapClientEntityLazyDelegate implements MapClientEntity {

    private final Supplier<MapClientEntity> delegateSupplier;

    private final AtomicMarkableReference<MapClientEntity> delegate = new AtomicMarkableReference<>(null, false);

    public MapClientEntityLazyDelegate(Supplier<MapClientEntity> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    protected MapClientEntity getWriteDelegate() {
        if (! isWriteDelegateInitialized()) {
            delegate.compareAndSet(null, delegateSupplier == null ? null : delegateSupplier.get(), false, true);
        }
        MapClientEntity ref = delegate.getReference();
        if (ref == null) {
            throw new IllegalStateException("Invalid delegate obtained");
        }
        return ref;
    }

    protected boolean isWriteDelegateInitialized() {
        return delegate.isMarked();
    }

    protected MapClientEntity getReadDelegate() {
        return getWriteDelegate();
    }

    @Override
    public void setClientScope(String id, Boolean defaultScope) {
        getWriteDelegate().setClientScope(id, defaultScope);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getWriteDelegate().addRedirectUri(redirectUri);
    }

    @Override
    public void addScopeMapping(String id) {
        getWriteDelegate().addScopeMapping(id);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getWriteDelegate().addWebOrigin(webOrigin);
    }

    @Override
    public void removeScopeMapping(String id) {
        getWriteDelegate().removeScopeMapping(id);
    }

    @Override
    public List<String> getAttribute(String name) {
        return getReadDelegate().getAttribute(name);
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return getReadDelegate().getAttributes();
    }

    @Override
    public Map<String, String> getAuthFlowBindings() {
        return getReadDelegate().getAuthFlowBindings();
    }

    @Override
    public String getAuthenticationFlowBindingOverride(String binding) {
        return getReadDelegate().getAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return getReadDelegate().getAuthenticationFlowBindingOverrides();
    }

    @Override
    public String getBaseUrl() {
        return getReadDelegate().getBaseUrl();
    }

    @Override
    public String getClientAuthenticatorType() {
        return getReadDelegate().getClientAuthenticatorType();
    }

    @Override
    public String getClientId() {
        return getReadDelegate().getClientId();
    }

    @Override
    public Stream<String> getClientScopes(boolean defaultScope) {
        return getReadDelegate().getClientScopes(defaultScope);
    }

    @Override
    public Map<String, Boolean> getClientScopes() {
        return getReadDelegate().getClientScopes();
    }

    @Override
    public String getDescription() {
        return getReadDelegate().getDescription();
    }

    @Override
    public String getManagementUrl() {
        return getReadDelegate().getManagementUrl();
    }

    @Override
    public String getName() {
        return getReadDelegate().getName();
    }

    @Override
    public Integer getNodeReRegistrationTimeout() {
        return getReadDelegate().getNodeReRegistrationTimeout();
    }

    @Override
    public Integer getNotBefore() {
        return getReadDelegate().getNotBefore();
    }

    @Override
    public String getProtocol() {
        return getReadDelegate().getProtocol();
    }

    @Override
    public ProtocolMapperModel getProtocolMapper(String id) {
        return getReadDelegate().getProtocolMapper(id);
    }

    @Override
    public Map<String,ProtocolMapperModel> getProtocolMappers() {
        return getReadDelegate().getProtocolMappers();
    }

    @Override
    public String getRealmId() {
        return getReadDelegate().getRealmId();
    }

    @Override
    public void setRealmId(String realmId) {
        getWriteDelegate().setRealmId(realmId);
    }

    @Override
    public Set<String> getRedirectUris() {
        return getReadDelegate().getRedirectUris();
    }

    @Override
    public String getRegistrationToken() {
        return getReadDelegate().getRegistrationToken();
    }

    @Override
    public String getRootUrl() {
        return getReadDelegate().getRootUrl();
    }

    @Override
    public Set<String> getScope() {
        return getReadDelegate().getScope();
    }

    @Override
    public Collection<String> getScopeMappings() {
        return getReadDelegate().getScopeMappings();
    }

    @Override
    public String getSecret() {
        return getReadDelegate().getSecret();
    }

    @Override
    public Set<String> getWebOrigins() {
        return getReadDelegate().getWebOrigins();
    }

    @Override
    public Boolean isAlwaysDisplayInConsole() {
        return getWriteDelegate().isAlwaysDisplayInConsole();
    }

    @Override
    public Boolean isBearerOnly() {
        return getWriteDelegate().isBearerOnly();
    }

    @Override
    public Boolean isConsentRequired() {
        return getWriteDelegate().isConsentRequired();
    }

    @Override
    public Boolean isDirectAccessGrantsEnabled() {
        return getWriteDelegate().isDirectAccessGrantsEnabled();
    }

    @Override
    public Boolean isEnabled() {
        return getWriteDelegate().isEnabled();
    }

    @Override
    public Boolean isFrontchannelLogout() {
        return getWriteDelegate().isFrontchannelLogout();
    }

    @Override
    public Boolean isFullScopeAllowed() {
        return getWriteDelegate().isFullScopeAllowed();
    }

    @Override
    public Boolean isImplicitFlowEnabled() {
        return getWriteDelegate().isImplicitFlowEnabled();
    }

    @Override
    public Boolean isPublicClient() {
        return getWriteDelegate().isPublicClient();
    }

    @Override
    public Boolean isServiceAccountsEnabled() {
        return getWriteDelegate().isServiceAccountsEnabled();
    }

    @Override
    public Boolean isStandardFlowEnabled() {
        return getWriteDelegate().isStandardFlowEnabled();
    }

    @Override
    public Boolean isSurrogateAuthRequired() {
        return getWriteDelegate().isSurrogateAuthRequired();
    }

    @Override
    public void removeAttribute(String name) {
        getWriteDelegate().removeAttribute(name);
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        getWriteDelegate().removeAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public void removeClientScope(String id) {
        getWriteDelegate().removeClientScope(id);
    }

    @Override
    public void removeProtocolMapper(String id) {
        getWriteDelegate().removeProtocolMapper(id);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getWriteDelegate().removeRedirectUri(redirectUri);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getWriteDelegate().removeWebOrigin(webOrigin);
    }

    @Override
    public void setAlwaysDisplayInConsole(Boolean alwaysDisplayInConsole) {
        getWriteDelegate().setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        getWriteDelegate().setAttribute(name, values);
    }

    @Override
    public void setAuthFlowBindings(Map<String, String> authFlowBindings) {
        getWriteDelegate().setAuthFlowBindings(authFlowBindings);
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        getWriteDelegate().setAuthenticationFlowBindingOverride(binding, flowId);
    }

    @Override
    public void setBaseUrl(String baseUrl) {
        getWriteDelegate().setBaseUrl(baseUrl);
    }

    @Override
    public void setBearerOnly(Boolean bearerOnly) {
        getWriteDelegate().setBearerOnly(bearerOnly);
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        getWriteDelegate().setClientAuthenticatorType(clientAuthenticatorType);
    }

    @Override
    public void setClientId(String clientId) {
        getWriteDelegate().setClientId(clientId);
    }

    @Override
    public void setConsentRequired(Boolean consentRequired) {
        getWriteDelegate().setConsentRequired(consentRequired);
    }

    @Override
    public void setDescription(String description) {
        getWriteDelegate().setDescription(description);
    }

    @Override
    public void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled) {
        getWriteDelegate().setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public void setEnabled(Boolean enabled) {
        getWriteDelegate().setEnabled(enabled);
    }

    @Override
    public void setFrontchannelLogout(Boolean frontchannelLogout) {
        getWriteDelegate().setFrontchannelLogout(frontchannelLogout);
    }

    @Override
    public void setFullScopeAllowed(Boolean fullScopeAllowed) {
        getWriteDelegate().setFullScopeAllowed(fullScopeAllowed);
    }

    @Override
    public void setImplicitFlowEnabled(Boolean implicitFlowEnabled) {
        getWriteDelegate().setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public void setManagementUrl(String managementUrl) {
        getWriteDelegate().setManagementUrl(managementUrl);
    }

    @Override
    public void setName(String name) {
        getWriteDelegate().setName(name);
    }

    @Override
    public void setNodeReRegistrationTimeout(Integer nodeReRegistrationTimeout) {
        getWriteDelegate().setNodeReRegistrationTimeout(nodeReRegistrationTimeout);
    }

    @Override
    public void setNotBefore(Integer notBefore) {
        getWriteDelegate().setNotBefore(notBefore);
    }

    @Override
    public void setProtocol(String protocol) {
        getWriteDelegate().setProtocol(protocol);
    }

    @Override
    public void setPublicClient(Boolean publicClient) {
        getWriteDelegate().setPublicClient(publicClient);
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        getWriteDelegate().setRedirectUris(redirectUris);
    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        getWriteDelegate().setRegistrationToken(registrationToken);
    }

    @Override
    public void setRootUrl(String rootUrl) {
        getWriteDelegate().setRootUrl(rootUrl);
    }

    @Override
    public void setScope(Set<String> scope) {
        getWriteDelegate().setScope(scope);
    }

    @Override
    public void setSecret(String secret) {
        getWriteDelegate().setSecret(secret);
    }

    @Override
    public void setServiceAccountsEnabled(Boolean serviceAccountsEnabled) {
        getWriteDelegate().setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public void setStandardFlowEnabled(Boolean standardFlowEnabled) {
        getWriteDelegate().setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public void setSurrogateAuthRequired(Boolean surrogateAuthRequired) {
        getWriteDelegate().setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        getWriteDelegate().setWebOrigins(webOrigins);
    }

    @Override
    public void setProtocolMapper(String id, ProtocolMapperModel mapping) {
        getWriteDelegate().setProtocolMapper(id, mapping);
    }

    @Override
    public String getId() {
        return getReadDelegate().getId();
    }

    @Override
    public void setId(String id) {
        getWriteDelegate().setId(id);
    }

    @Override
    public boolean isUpdated() {
        return isWriteDelegateInitialized() && getWriteDelegate().isUpdated();
    }

}
