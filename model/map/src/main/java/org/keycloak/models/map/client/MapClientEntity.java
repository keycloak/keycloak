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
import org.keycloak.models.map.common.AbstractEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 *
 * @author hmlnarik
 */
public interface MapClientEntity<K> extends AbstractEntity<K> {

    void addClientScope(String id, Boolean defaultScope);

    ProtocolMapperModel addProtocolMapper(ProtocolMapperModel model);

    void addRedirectUri(String redirectUri);

    void addScopeMapping(String id);

    void addWebOrigin(String webOrigin);

    void deleteScopeMapping(String id);

    List<String> getAttribute(String name);

    Map<String, List<String>> getAttributes();

    Map<String, String> getAuthFlowBindings();

    String getAuthenticationFlowBindingOverride(String binding);

    Map<String, String> getAuthenticationFlowBindingOverrides();

    String getBaseUrl();

    String getClientAuthenticatorType();

    String getClientId();

    Stream<String> getClientScopes(boolean defaultScope);

    String getDescription();

    String getManagementUrl();

    String getName();

    int getNodeReRegistrationTimeout();

    int getNotBefore();

    String getProtocol();

    ProtocolMapperModel getProtocolMapperById(String id);

    Collection<ProtocolMapperModel> getProtocolMappers();

    String getRealmId();

    Set<String> getRedirectUris();

    String getRegistrationToken();

    String getRootUrl();

    Set<String> getScope();

    Collection<String> getScopeMappings();

    String getSecret();

    Set<String> getWebOrigins();

    Boolean isAlwaysDisplayInConsole();

    Boolean isBearerOnly();

    Boolean isConsentRequired();

    Boolean isDirectAccessGrantsEnabled();

    Boolean isEnabled();

    Boolean isFrontchannelLogout();

    Boolean isFullScopeAllowed();

    Boolean isImplicitFlowEnabled();

    Boolean isPublicClient();

    Boolean isServiceAccountsEnabled();

    Boolean isStandardFlowEnabled();

    Boolean isSurrogateAuthRequired();

    void removeAttribute(String name);

    void removeAuthenticationFlowBindingOverride(String binding);

    void removeClientScope(String id);

    void removeProtocolMapper(String id);

    void removeRedirectUri(String redirectUri);

    void removeWebOrigin(String webOrigin);

    void setAlwaysDisplayInConsole(Boolean alwaysDisplayInConsole);

    void setAttribute(String name, List<String> values);

    void setAuthFlowBindings(Map<String, String> authFlowBindings);

    void setAuthenticationFlowBindingOverride(String binding, String flowId);

    void setBaseUrl(String baseUrl);

    void setBearerOnly(Boolean bearerOnly);

    void setClientAuthenticatorType(String clientAuthenticatorType);

    void setClientId(String clientId);

    void setConsentRequired(Boolean consentRequired);

    void setDescription(String description);

    void setDirectAccessGrantsEnabled(Boolean directAccessGrantsEnabled);

    void setEnabled(Boolean enabled);

    void setFrontchannelLogout(Boolean frontchannelLogout);

    void setFullScopeAllowed(Boolean fullScopeAllowed);

    void setImplicitFlowEnabled(Boolean implicitFlowEnabled);

    void setManagementUrl(String managementUrl);

    void setName(String name);

    void setNodeReRegistrationTimeout(int nodeReRegistrationTimeout);

    void setNotBefore(int notBefore);

    void setProtocol(String protocol);

    void setProtocolMappers(Collection<ProtocolMapperModel> protocolMappers);

    void setPublicClient(Boolean publicClient);

    void setRedirectUris(Set<String> redirectUris);

    void setRegistrationToken(String registrationToken);

    void setRootUrl(String rootUrl);

    void setScope(Set<String> scope);

    void setSecret(String secret);

    void setServiceAccountsEnabled(Boolean serviceAccountsEnabled);

    void setStandardFlowEnabled(Boolean standardFlowEnabled);

    void setSurrogateAuthRequired(Boolean surrogateAuthRequired);

    void setWebOrigins(Set<String> webOrigins);

    void updateProtocolMapper(String id, ProtocolMapperModel mapping);

}
