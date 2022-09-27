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
import java.util.Objects;
import java.util.Set;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventManager;
import org.keycloak.storage.SearchableModelField;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientModel extends ClientScopeModel, RoleContainerModel,  ProtocolMapperContainerModel, ScopeContainerModel {

    // COMMON ATTRIBUTES

    String PRIVATE_KEY = "privateKey";
    String PUBLIC_KEY = "publicKey";
    String X509CERTIFICATE = "X509Certificate";
    String LOGO_URI ="logoUri";
    String POLICY_URI ="policyUri";
    String TOS_URI ="tosUri";

    public static class SearchableFields {
        public static final SearchableModelField<ClientModel> ID                 = new SearchableModelField<>("id", String.class);
        public static final SearchableModelField<ClientModel> REALM_ID           = new SearchableModelField<>("realmId", String.class);
        public static final SearchableModelField<ClientModel> CLIENT_ID          = new SearchableModelField<>("clientId", String.class);
        public static final SearchableModelField<ClientModel> ENABLED            = new SearchableModelField<>("enabled", Boolean.class);
        public static final SearchableModelField<ClientModel> SCOPE_MAPPING_ROLE = new SearchableModelField<>("scopeMappingRole", String.class);
        public static final SearchableModelField<ClientModel> ALWAYS_DISPLAY_IN_CONSOLE = new SearchableModelField<>("alwaysDisplayInConsole", Boolean.class);

        /**
         * Search for attribute value. The parameters is a pair {@code (attribute_name, value)} where {@code attribute_name}
         * is always checked for equality, and the value is checked per the operator.
         */
        public static final SearchableModelField<ClientModel> ATTRIBUTE          = new SearchableModelField<>("attribute", String[].class);
    }

    interface ClientCreationEvent extends ProviderEvent {
        ClientModel getCreatedClient();
    }

    // Called also during client creation after client is fully initialized (including all attributes etc)
    interface ClientUpdatedEvent extends ProviderEvent {
        ClientModel getUpdatedClient();
        KeycloakSession getKeycloakSession();
    }

    interface ClientRemovedEvent extends ProviderEvent {
        ClientModel getClient();
        KeycloakSession getKeycloakSession();
    }

    interface ClientProtocolUpdatedEvent extends ProviderEvent {
        ClientModel getClient();
    }

    /**
     * Notifies other providers that this client has been updated.
     * <p>
     * After a client is updated, providers can register for {@link ClientUpdatedEvent}.
     * The setters in this model do not send an update for individual updates of the model.
     * This method is here to allow for sending this event for this client,
     * allowsing for to group multiple changes of a client and signal that
     * all the changes in this client have been performed.
     *
     * @deprecated Do not use, to be removed
     *
     * @see ProviderEvent
     * @see ProviderEventManager
     * @see ClientUpdatedEvent
     */
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

    @Override
    default boolean hasDirectScope(RoleModel role) {
        if (getScopeMappingsStream().anyMatch(r -> Objects.equals(r, role))) return true;

        return getRolesStream().anyMatch(r -> Objects.equals(r, role));
    }

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

    /**
     * Add clientScopes with this client. Add as default scopes (if parameter 'defaultScope' is true) or optional scopes (if parameter 'defaultScope' is false)
     * @param clientScopes
     * @param defaultScope
     */
    void addClientScopes(Set<ClientScopeModel> clientScopes, boolean defaultScope);

    void removeClientScope(ClientScopeModel clientScope);

    /**
     * Return all default scopes (if 'defaultScope' is true) or all optional scopes (if 'defaultScope' is false) linked with this client
     *
     * @param defaultScope
     * @return map where key is the name of the clientScope, value is particular clientScope. Returns empty map if no scopes linked (never returns null).
     */
    Map<String, ClientScopeModel> getClientScopes(boolean defaultScope);

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
