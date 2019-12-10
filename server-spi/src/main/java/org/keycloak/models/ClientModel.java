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

package org.keycloak.models;

import java.util.Map;
import java.util.Set;

import org.keycloak.common.util.ObjectUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientModel extends ClientScopeModel, RoleContainerModel,  ProtocolMapperContainerModel, ScopeContainerModel {

    // COMMON ATTRIBUTES

    String PRIVATE_KEY = "privateKey";
    String PUBLIC_KEY = "publicKey";
    String X509CERTIFICATE = "X509Certificate";

    void updateClient();

    /**
     * Returns client internal ID (UUID).
     * @return
     */
    String getId();

    /**
     * Returns client ID as defined by the user.
     * @return
     */
    String getClientId();

    void setClientId(String clientId);

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isAlwaysDisplayInConsole();

    void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole);

    boolean isSurrogateAuthRequired();

    void setSurrogateAuthRequired(boolean surrogateAuthRequired);

    Set<String> getWebOrigins();

    void setWebOrigins(Set<String> webOrigins);

    void addWebOrigin(String webOrigin);

    void removeWebOrigin(String webOrigin);

    Set<String> getRedirectUris();

    void setRedirectUris(Set<String> redirectUris);

    void addRedirectUri(String redirectUri);

    void removeRedirectUri(String redirectUri);

    String getManagementUrl();

    void setManagementUrl(String url);

    String getRootUrl();

    void setRootUrl(String url);

    String getBaseUrl();

    void setBaseUrl(String url);


    boolean isBearerOnly();
    void setBearerOnly(boolean only);

    int getNodeReRegistrationTimeout();

    void setNodeReRegistrationTimeout(int timeout);

    String getClientAuthenticatorType();
    void setClientAuthenticatorType(String clientAuthenticatorType);

    boolean validateSecret(String secret);
    String getSecret();
    public void setSecret(String secret);

    String getRegistrationToken();
    void setRegistrationToken(String registrationToken);

    String getProtocol();
    void setProtocol(String protocol);

    void setAttribute(String name, String value);
    void removeAttribute(String name);
    String getAttribute(String name);
    Map<String, String> getAttributes();

    /**
     * Get authentication flow binding override for this client.  Allows client to override an authentication flow binding.
     *
     * @param binding examples are "browser", "direct_grant"
     *
     * @return
     */
    String getAuthenticationFlowBindingOverride(String binding);
    Map<String, String> getAuthenticationFlowBindingOverrides();
    void removeAuthenticationFlowBindingOverride(String binding);
    void setAuthenticationFlowBindingOverride(String binding, String flowId);

    boolean isFrontchannelLogout();
    void setFrontchannelLogout(boolean flag);

    boolean isFullScopeAllowed();
    void setFullScopeAllowed(boolean value);


    boolean isPublicClient();
    void setPublicClient(boolean flag);

    boolean isConsentRequired();
    void setConsentRequired(boolean consentRequired);

    boolean isStandardFlowEnabled();
    void setStandardFlowEnabled(boolean standardFlowEnabled);

    boolean isImplicitFlowEnabled();
    void setImplicitFlowEnabled(boolean implicitFlowEnabled);

    boolean isDirectAccessGrantsEnabled();
    void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled);

    boolean isServiceAccountsEnabled();
    void setServiceAccountsEnabled(boolean serviceAccountsEnabled);

    RealmModel getRealm();

    /**
     * Add clientScope with this client. Add it as default scope (if parameter 'defaultScope' is true) or optional scope (if parameter 'defaultScope' is false)
     * @param clientScope
     * @param defaultScope
     */
    void addClientScope(ClientScopeModel clientScope, boolean defaultScope);

    void removeClientScope(ClientScopeModel clientScope);

    /**
     * Return all default scopes (if 'defaultScope' is true) or all optional scopes (if 'defaultScope' is false) linked with this client
     *
     * @param defaultScope
     * @param filterByProtocol if true, then just client scopes of same protocol like current client will be returned
     * @return map where key is the name of the clientScope, value is particular clientScope. Returns empty map if no scopes linked (never returns null).
     */
    Map<String, ClientScopeModel> getClientScopes(boolean defaultScope, boolean filterByProtocol);

    /**
     * <p>Returns a {@link ClientScopeModel} associated with this client.
     *
     * <p>This method is used as a fallback in order to let clients to resolve a {@code scope} dynamically which is not listed as default or optional scope when calling {@link #getClientScopes(boolean, boolean)}.
     *
     * @param scope the scope name
     * @return the client scope
     */
    default ClientScopeModel getDynamicClientScope(String scope) {
        return null;
    }

    /**
     * Time in seconds since epoc
     *
     * @return
     */
    int getNotBefore();

    void setNotBefore(int notBefore);

     Map<String, Integer> getRegisteredNodes();

    /**
     * Register node or just update the 'lastReRegistration' time if this node is already registered
     *
     * @param nodeHost
     * @param registrationTime
     */
    void registerNode(String nodeHost, int registrationTime);

    void unregisterNode(String nodeHost);


    // Clients are not displayed on consent screen by default
    @Override
    default boolean isDisplayOnConsentScreen() {
        String displayVal = getAttribute(DISPLAY_ON_CONSENT_SCREEN);
        return displayVal==null ? false : Boolean.parseBoolean(displayVal);
    }

    // Fallback to name or clientId if consentScreenText attribute is null
    @Override
    default String getConsentScreenText() {
        String consentScreenText = ClientScopeModel.super.getConsentScreenText();
        if (ObjectUtil.isBlank(consentScreenText)) {
            consentScreenText = getClientId();
        }
        return consentScreenText;
    }
}
