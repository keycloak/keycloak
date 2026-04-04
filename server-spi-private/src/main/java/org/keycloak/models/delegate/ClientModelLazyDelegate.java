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
package org.keycloak.models.delegate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelIllegalStateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

/**
 *
 * @author hmlnarik
 */
public class ClientModelLazyDelegate implements ClientModel {

    private final Supplier<ClientModel> delegateSupplier;

    private final AtomicMarkableReference<ClientModel> delegate = new AtomicMarkableReference<>(null, false);

    public static class WithId extends ClientModelLazyDelegate {

        private final String id;

        public WithId(String id, Supplier<ClientModel> delegateSupplier) {
            super(delegateSupplier);
            this.id = id;
        }

        public WithId(KeycloakSession session, RealmModel realm, String id) {
            super(() -> session.clients().getClientById(realm, id));
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
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
    }

    public ClientModelLazyDelegate(Supplier<ClientModel> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    private ClientModel getDelegate() {
        if (! delegate.isMarked()) {
            delegate.compareAndSet(null, delegateSupplier == null ? null : delegateSupplier.get(), false, true);
        }
        ClientModel ref = delegate.getReference();
        if (ref == null) {
            throw new ModelIllegalStateException("Invalid delegate obtained");
        }
        return ref;
    }

    @Override
    public void updateClient() {
        getDelegate().updateClient();
    }

    @Override
    public String getId() {
        return getDelegate().getId();
    }

    @Override
    public String getClientId() {
        return getDelegate().getClientId();
    }

    @Override
    public void setClientId(String clientId) {
        getDelegate().setClientId(clientId);
    }

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public void setName(String name) {
        getDelegate().setName(name);
    }

    @Override
    public String getDescription() {
        return getDelegate().getDescription();
    }

    @Override
    public void setDescription(String description) {
        getDelegate().setDescription(description);
    }

    @Override
    public boolean isEnabled() {
        return getDelegate().isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        getDelegate().setEnabled(enabled);
    }

    @Override
    public boolean isAlwaysDisplayInConsole() {
        return getDelegate().isAlwaysDisplayInConsole();
    }

    @Override
    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        getDelegate().setAlwaysDisplayInConsole(alwaysDisplayInConsole);
    }

    @Override
    public boolean isSurrogateAuthRequired() {
        return getDelegate().isSurrogateAuthRequired();
    }

    @Override
    public void setSurrogateAuthRequired(boolean surrogateAuthRequired) {
        getDelegate().setSurrogateAuthRequired(surrogateAuthRequired);
    }

    @Override
    public Set<String> getWebOrigins() {
        return getDelegate().getWebOrigins();
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        getDelegate().setWebOrigins(webOrigins);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        getDelegate().addWebOrigin(webOrigin);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        getDelegate().removeWebOrigin(webOrigin);
    }

    @Override
    public Set<String> getRedirectUris() {
        return getDelegate().getRedirectUris();
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        getDelegate().setRedirectUris(redirectUris);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        getDelegate().addRedirectUri(redirectUri);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        getDelegate().removeRedirectUri(redirectUri);
    }

    @Override
    public String getManagementUrl() {
        return getDelegate().getManagementUrl();
    }

    @Override
    public void setManagementUrl(String url) {
        getDelegate().setManagementUrl(url);
    }

    @Override
    public String getRootUrl() {
        return getDelegate().getRootUrl();
    }

    @Override
    public void setRootUrl(String url) {
        getDelegate().setRootUrl(url);
    }

    @Override
    public String getBaseUrl() {
        return getDelegate().getBaseUrl();
    }

    @Override
    public void setBaseUrl(String url) {
        getDelegate().setBaseUrl(url);
    }

    @Override
    public boolean isBearerOnly() {
        return getDelegate().isBearerOnly();
    }

    @Override
    public void setBearerOnly(boolean only) {
        getDelegate().setBearerOnly(only);
    }

    @Override
    public int getNodeReRegistrationTimeout() {
        return getDelegate().getNodeReRegistrationTimeout();
    }

    @Override
    public void setNodeReRegistrationTimeout(int timeout) {
        getDelegate().setNodeReRegistrationTimeout(timeout);
    }

    @Override
    public String getClientAuthenticatorType() {
        return getDelegate().getClientAuthenticatorType();
    }

    @Override
    public void setClientAuthenticatorType(String clientAuthenticatorType) {
        getDelegate().setClientAuthenticatorType(clientAuthenticatorType);
    }

    @Override
    public boolean validateSecret(String secret) {
        return getDelegate().validateSecret(secret);
    }

    @Override
    public String getSecret() {
        return getDelegate().getSecret();
    }

    @Override
    public void setSecret(String secret) {
        getDelegate().setSecret(secret);
    }

    @Override
    public String getRegistrationToken() {
        return getDelegate().getRegistrationToken();
    }

    @Override
    public void setRegistrationToken(String registrationToken) {
        getDelegate().setRegistrationToken(registrationToken);
    }

    @Override
    public String getProtocol() {
        return getDelegate().getProtocol();
    }

    @Override
    public void setProtocol(String protocol) {
        getDelegate().setProtocol(protocol);
    }

    @Override
    public void setAttribute(String name, String value) {
        getDelegate().setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        getDelegate().removeAttribute(name);
    }

    @Override
    public String getAttribute(String name) {
        return getDelegate().getAttribute(name);
    }

    @Override
    public Map<String, String> getAttributes() {
        return getDelegate().getAttributes();
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
    public void removeAuthenticationFlowBindingOverride(String binding) {
        getDelegate().removeAuthenticationFlowBindingOverride(binding);
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        getDelegate().setAuthenticationFlowBindingOverride(binding, flowId);
    }

    @Override
    public boolean isFrontchannelLogout() {
        return getDelegate().isFrontchannelLogout();
    }

    @Override
    public void setFrontchannelLogout(boolean flag) {
        getDelegate().setFrontchannelLogout(flag);
    }

    @Override
    public boolean isFullScopeAllowed() {
        return getDelegate().isFullScopeAllowed();
    }

    @Override
    public void setFullScopeAllowed(boolean value) {
        getDelegate().setFullScopeAllowed(value);
    }

    @Override
    public boolean isPublicClient() {
        return getDelegate().isPublicClient();
    }

    @Override
    public void setPublicClient(boolean flag) {
        getDelegate().setPublicClient(flag);
    }

    @Override
    public boolean isConsentRequired() {
        return getDelegate().isConsentRequired();
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        getDelegate().setConsentRequired(consentRequired);
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return getDelegate().isStandardFlowEnabled();
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        getDelegate().setStandardFlowEnabled(standardFlowEnabled);
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return getDelegate().isImplicitFlowEnabled();
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        getDelegate().setImplicitFlowEnabled(implicitFlowEnabled);
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return getDelegate().isDirectAccessGrantsEnabled();
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        getDelegate().setDirectAccessGrantsEnabled(directAccessGrantsEnabled);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return getDelegate().isServiceAccountsEnabled();
    }

    @Override
    public void setServiceAccountsEnabled(boolean serviceAccountsEnabled) {
        getDelegate().setServiceAccountsEnabled(serviceAccountsEnabled);
    }

    @Override
    public RealmModel getRealm() {
        return getDelegate().getRealm();
    }

    @Override
    public void addClientScope(ClientScopeModel clientScope, boolean defaultScope) {
        getDelegate().addClientScope(clientScope, defaultScope);
    }

    @Override
    public void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        getDelegate().addClientScopes(clientScopes, defaultScope);
    }

    @Override
    public void removeClientScope(ClientScopeModel clientScope) {
        getDelegate().removeClientScope(clientScope);
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(boolean defaultScope) {
        return getDelegate().getClientScopes(defaultScope);
    }

    @Override
    public ClientScopeModel getDynamicClientScope(String scope) {
        return getDelegate().getDynamicClientScope(scope);
    }

    @Override
    public int getNotBefore() {
        return getDelegate().getNotBefore();
    }

    @Override
    public void setNotBefore(int notBefore) {
        getDelegate().setNotBefore(notBefore);
    }

    @Override
    public Map<String, Integer> getRegisteredNodes() {
        return getDelegate().getRegisteredNodes();
    }

    @Override
    public void registerNode(String nodeHost, int registrationTime) {
        getDelegate().registerNode(nodeHost, registrationTime);
    }

    @Override
    public void unregisterNode(String nodeHost) {
        getDelegate().unregisterNode(nodeHost);
    }

    @Override
    public boolean isDisplayOnConsentScreen() {
        return getDelegate().isDisplayOnConsentScreen();
    }

    @Override
    public String getConsentScreenText() {
        return getDelegate().getConsentScreenText();
    }

    @Override
    public void setDisplayOnConsentScreen(boolean displayOnConsentScreen) {
        getDelegate().setDisplayOnConsentScreen(displayOnConsentScreen);
    }

    @Override
    public void setConsentScreenText(String consentScreenText) {
        getDelegate().setConsentScreenText(consentScreenText);
    }

    @Override
    public String getGuiOrder() {
        return getDelegate().getGuiOrder();
    }

    @Override
    public void setGuiOrder(String guiOrder) {
        getDelegate().setGuiOrder(guiOrder);
    }

    @Override
    public boolean isIncludeInTokenScope() {
        return getDelegate().isIncludeInTokenScope();
    }

    @Override
    public void setIncludeInTokenScope(boolean includeInTokenScope) {
        getDelegate().setIncludeInTokenScope(includeInTokenScope);
    }

    @Override
    public boolean isDynamicScope() {
        return getDelegate().isDynamicScope();
    }

    @Override
    public void setIsDynamicScope(boolean isDynamicScope) {
        getDelegate().setIsDynamicScope(isDynamicScope);
    }

    @Override
    public String getDynamicScopeRegexp() {
        return getDelegate().getDynamicScopeRegexp();
    }

    @Override
    public boolean isIncludeInOpenIDProviderMetadata() {
        return getDelegate().isIncludeInOpenIDProviderMetadata();
    }

    @Override
    public void setIncludeInOpenIDProviderMetadata(boolean includeInOpenIDProviderMetadata) {
        getDelegate().setIncludeInOpenIDProviderMetadata(includeInOpenIDProviderMetadata);
    }

    @Override
    public Stream<RoleModel> getScopeMappingsStream() {
        return getDelegate().getScopeMappingsStream();
    }

    @Override
    public Stream<RoleModel> getRealmScopeMappingsStream() {
        return getDelegate().getRealmScopeMappingsStream();
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        getDelegate().addScopeMapping(role);
    }

    @Override
    public void deleteScopeMapping(RoleModel role) {
        getDelegate().deleteScopeMapping(role);
    }

    @Override
    public boolean hasDirectScope(RoleModel role) {
        return getDelegate().hasDirectScope(role);
    }

    @Override
    public boolean hasScope(RoleModel role) {
        return getDelegate().hasScope(role);
    }

    @Override
    public RoleModel getRole(String name) {
        return getDelegate().getRole(name);
    }

    @Override
    public RoleModel addRole(String name) {
        return getDelegate().addRole(name);
    }

    @Override
    public RoleModel addRole(String id, String name) {
        return getDelegate().addRole(id, name);
    }

    @Override
    public boolean removeRole(RoleModel role) {
        return getDelegate().removeRole(role);
    }

    @Override
    public Stream<RoleModel> getRolesStream() {
        return getDelegate().getRolesStream();
    }

    @Override
    public Stream<RoleModel> getRolesStream(Integer firstResult, Integer maxResults) {
        return getDelegate().getRolesStream(firstResult, maxResults);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(String search, Integer first, Integer max) {
        return getDelegate().searchForRolesStream(search, first, max);
    }

    @Override
    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        return getDelegate().getProtocolMappersStream();
    }

    @Override
    public ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model) {
        return getDelegate().addProtocolMapper(model);
    }

    @Override
    public void removeProtocolMapper(ProtocolMapperModel mapping) {
        getDelegate().removeProtocolMapper(mapping);
    }

    @Override
    public void updateProtocolMapper(ProtocolMapperModel mapping) {
        getDelegate().updateProtocolMapper(mapping);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperById(String id) {
        return getDelegate().getProtocolMapperById(id);
    }

    @Override
    public List<ProtocolMapperModel> getProtocolMapperByType(String type) {
        return getDelegate().getProtocolMapperByType(type);
    }

    @Override
    public ProtocolMapperModel getProtocolMapperByName(String protocol, String name) {
        return getDelegate().getProtocolMapperByName(protocol, name);
    }

}
