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

import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.HotRodPair;
import org.keycloak.models.map.common.Versioned;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HotRodClientEntity implements MapClientEntity, Versioned {

    @ProtoField(number = 1, required = true)
    public int entityVersion = 1;

    @ProtoField(number = 2, required = true)
    public String id;

    @ProtoField(number = 3)
    public String realmId;

    @ProtoField(number = 4)
    public String clientId;

    @ProtoField(number = 5)
    public String name;

    @ProtoField(number = 6)
    public String description;

    @ProtoField(number = 7)
    public Set<String> redirectUris = new HashSet<>();

    @ProtoField(number = 8)
    public Boolean enabled;

    @ProtoField(number = 9)
    public Boolean alwaysDisplayInConsole;

    @ProtoField(number = 10)
    public String clientAuthenticatorType;

    @ProtoField(number = 11)
    public String secret;

    @ProtoField(number = 12)
    public String registrationToken;

    @ProtoField(number = 13)
    public String protocol;

    @ProtoField(number = 14)
    public Set<HotRodAttributeEntity> attributes = new HashSet<>();

    @ProtoField(number = 15)
    public Set<HotRodPair<String, String>> authFlowBindings = new HashSet<>();

    @ProtoField(number = 16)
    public Boolean publicClient;

    @ProtoField(number = 17)
    public Boolean fullScopeAllowed;

    @ProtoField(number = 18)
    public Boolean frontchannelLogout;

    @ProtoField(number = 19)
    public Integer notBefore;

    @ProtoField(number = 20)
    public Set<String> scope = new HashSet<>();

    @ProtoField(number = 21)
    public Set<String> webOrigins = new HashSet<>();

    @ProtoField(number = 22)
    public Set<HotRodProtocolMapperEntity> protocolMappers = new HashSet<>();

    @ProtoField(number = 23)
    public Set<HotRodPair<String, Boolean>> clientScopes = new HashSet<>();

    @ProtoField(number = 24)
    public Set<String> scopeMappings = new HashSet<>();

    @ProtoField(number = 25)
    public Boolean surrogateAuthRequired;

    @ProtoField(number = 26)
    public String managementUrl;

    @ProtoField(number = 27)
    public String baseUrl;

    @ProtoField(number = 28)
    public Boolean bearerOnly;

    @ProtoField(number = 29)
    public Boolean consentRequired;

    @ProtoField(number = 30)
    public String rootUrl;

    @ProtoField(number = 31)
    public Boolean standardFlowEnabled;

    @ProtoField(number = 32)
    public Boolean implicitFlowEnabled;

    @ProtoField(number = 33)
    public Boolean directAccessGrantsEnabled;

    @ProtoField(number = 34)
    public Boolean serviceAccountsEnabled;

    @ProtoField(number = 35)
    public Integer nodeReRegistrationTimeout;

    private boolean updated = false;

    private final DeepCloner cloner;

    public HotRodClientEntity() {
        this(DeepCloner.DUMB_CLONER);
    }

    public HotRodClientEntity(DeepCloner cloner) {
        this.cloner = cloner;
    }

    @Override
    public int getEntityVersion() {
        return entityVersion;
    }

    @Override
    public List<String> getAttribute(String name) {
        return attributes.stream()
                .filter(attributeEntity -> Objects.equals(attributeEntity.getName(), name))
                .findFirst()
                .map(HotRodAttributeEntity::getValues)
                .orElse(Collections.emptyList());
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        return attributes.stream().collect(Collectors.toMap(HotRodAttributeEntity::getName, HotRodAttributeEntity::getValues));
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        boolean valueUndefined = values == null || values.isEmpty();

        Optional<HotRodAttributeEntity> first = attributes.stream()
                .filter(attributeEntity -> Objects.equals(attributeEntity.getName(), name))
                .findFirst();

        if (first.isPresent()) {
            HotRodAttributeEntity attributeEntity = first.get();
            if (valueUndefined) {
                this.updated = true;
                removeAttribute(name);
            } else {
                this.updated |= !Objects.equals(attributeEntity.getValues(), values);
                attributeEntity.setValues(values);
            }

            return;
        }

        // do not create attributes if empty or null
        if (valueUndefined) {
            return;
        }

        HotRodAttributeEntity newAttributeEntity = new HotRodAttributeEntity(name, values);
        updated |= attributes.add(newAttributeEntity);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.stream()
                .filter(attributeEntity -> Objects.equals(attributeEntity.getName(), name))
                .findFirst()
                .ifPresent(attr -> {
                    this.updated |= attributes.remove(attr);
                });
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
        if (redirectUris == null || redirectUris.isEmpty()) {
            this.updated |= !this.redirectUris.isEmpty();
            this.redirectUris.clear();
            return;
        }

        this.updated |= ! Objects.equals(this.redirectUris, redirectUris);
        this.redirectUris.clear();
        this.redirectUris.addAll(redirectUris);
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
    public Map<String, String> getAuthFlowBindings() {
        return authFlowBindings.stream().collect(Collectors.toMap(HotRodPair::getFirst, HotRodPair::getSecond));
    }

    @Override
    public void setAuthFlowBindings(Map<String, String> authFlowBindings) {
        if (authFlowBindings == null || authFlowBindings.isEmpty()) {
            this.updated |= !this.authFlowBindings.isEmpty();
            this.authFlowBindings.clear();
            return;
        }

        this.updated = true;
        this.authFlowBindings.clear();
        this.authFlowBindings.addAll(authFlowBindings.entrySet().stream().map(e -> new HotRodPair<>(e.getKey(), e.getValue())).collect(Collectors.toSet()));
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
    public void setRealmId(String realmId) {
        this.realmId = realmId;
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
    public Integer getNotBefore() {
        return notBefore;
    }

    @Override
    public void setNotBefore(Integer notBefore) {
        this.updated |= ! Objects.equals(this.notBefore, notBefore);
        this.notBefore = notBefore;
    }

    @Override
    public Set<String> getScope() {
        return scope;
    }

    @Override
    public void setScope(Set<String> scope) {
        if (scope == null || scope.isEmpty()) {
            this.updated |= !this.scope.isEmpty();
            this.scope.clear();
            return;
        }

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
        if (webOrigins == null || webOrigins.isEmpty()) {
            this.updated |= !this.webOrigins.isEmpty();
            this.webOrigins.clear();
            return;
        }

        this.updated |= ! Objects.equals(this.webOrigins, webOrigins);
        this.webOrigins.clear();
        this.webOrigins.addAll(webOrigins);
    }

    @Override
    public Map<String, MapProtocolMapperEntity> getProtocolMappers() {
        return protocolMappers.stream().collect(Collectors.toMap(HotRodProtocolMapperEntity::getId, Function.identity()));
    }


    @Override
    public MapProtocolMapperEntity getProtocolMapper(String id) {
        return protocolMappers.stream().filter(hotRodMapper -> Objects.equals(hotRodMapper.getId(), id)).findFirst().orElse(null);
    }

    @Override
    public void setProtocolMapper(String id, MapProtocolMapperEntity mapping) {
        removeProtocolMapper(id);

        protocolMappers.add((HotRodProtocolMapperEntity) cloner.from(mapping)); // Workaround, will be replaced by cloners
        this.updated = true;
    }

    @Override
    public void removeProtocolMapper(String id) {
        protocolMappers.stream().filter(entity -> Objects.equals(id, entity.id))
                .findFirst()
                .ifPresent(entity -> {
                    protocolMappers.remove(entity);
                    updated = true;
                });
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
    public Integer getNodeReRegistrationTimeout() {
        return nodeReRegistrationTimeout;
    }

    @Override
    public void setNodeReRegistrationTimeout(Integer nodeReRegistrationTimeout) {
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
    public String getAuthenticationFlowBindingOverride(String binding) {
        return authFlowBindings.stream().filter(pair -> Objects.equals(pair.getFirst(), binding)).findFirst()
                .map(HotRodPair::getSecond)
                .orElse(null);
    }

    @Override
    public Map<String, String> getAuthenticationFlowBindingOverrides() {
        return this.authFlowBindings.stream().collect(Collectors.toMap(HotRodPair::getFirst, HotRodPair::getSecond));
    }

    @Override
    public void removeAuthenticationFlowBindingOverride(String binding) {
        this.authFlowBindings.stream().filter(pair -> Objects.equals(pair.getFirst(), binding)).findFirst()
                .ifPresent(pair -> {
                    updated = true;
                    authFlowBindings.remove(pair);
                });
    }

    @Override
    public void setAuthenticationFlowBindingOverride(String binding, String flowId) {
        this.updated = true;
        
        removeAuthenticationFlowBindingOverride(binding);
        
        this.authFlowBindings.add(new HotRodPair<>(binding, flowId));
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
    public void removeScopeMapping(String id) {
        updated |= scopeMappings.remove(id);
    }

    @Override
    public Map<String, Boolean> getClientScopes() {
        return this.clientScopes.stream().collect(Collectors.toMap(HotRodPair::getFirst, HotRodPair::getSecond));
    }

    @Override
    public void setClientScope(String id, Boolean defaultScope) {
        if (id != null) {
            updated = true;
            removeClientScope(id);

            this.clientScopes.add(new HotRodPair<>(id, defaultScope));
        }
    }

    @Override
    public void removeClientScope(String id) {
        this.clientScopes.stream().filter(pair -> Objects.equals(pair.getFirst(), id)).findFirst()
                .ifPresent(pair -> {
                    updated = true;
                    clientScopes.remove(pair);
                });
    }

    @Override
    public Stream<String> getClientScopes(boolean defaultScope) {
        return this.clientScopes.stream()
                .filter(pair -> Objects.equals(pair.getSecond(), defaultScope))
                .map(HotRodPair::getFirst);
    }

    @Override
    public String getRealmId() {
        return this.realmId;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        if (this.id != null) throw new IllegalStateException("Id cannot be changed");
        this.id = id;
        this.updated |= id != null;
    }

    @Override
    public void clearUpdatedFlag() {
        this.updated = false;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }
}
