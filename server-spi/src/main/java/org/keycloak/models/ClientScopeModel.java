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

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.provider.ProviderEvent;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientScopeModel extends ProtocolMapperContainerModel, ScopeContainerModel, OrderedModel {

    /**
     * The character separator used to specify values when the client scope is dynamic. For instance, {@code <scope>:<value>}.
     */
    String VALUE_SEPARATOR = ":";

    interface ClientScopeRemovedEvent extends ProviderEvent {
        ClientScopeModel getClientScope();

        KeycloakSession getKeycloakSession();
    }

    interface ClientScopeCreatedEvent extends ProviderEvent {
        ClientScopeModel getClientScope();

        KeycloakSession getKeycloakSession();
    }

    String getId();

    String getName();

    RealmModel getRealm();
    void setName(String name);

    String getDescription();

    void setDescription(String description);

    String getProtocol();
    void setProtocol(String protocol);

    void setAttribute(String name, String value);
    void removeAttribute(String name);
    String getAttribute(String name);
    Map<String, String> getAttributes();


    // CONFIGS

    String DISPLAY_ON_CONSENT_SCREEN = "display.on.consent.screen";
    String CONSENT_SCREEN_TEXT = "consent.screen.text";
    String GUI_ORDER = "gui.order";
    String INCLUDE_IN_TOKEN_SCOPE = "include.in.token.scope";
    String IS_DYNAMIC_SCOPE = "is.dynamic.scope";
    String DYNAMIC_SCOPE_REGEXP = "dynamic.scope.regexp";
    String HIDE_FROM_OPENID_PROVIDER_METADATA = "hide.from.openid.provider.metadata";

    default boolean isDisplayOnConsentScreen() {
        String displayVal = getAttribute(DISPLAY_ON_CONSENT_SCREEN);
        return displayVal==null ? true : Boolean.parseBoolean(displayVal);
    }

    default void setDisplayOnConsentScreen(boolean displayOnConsentScreen) {
        setAttribute(DISPLAY_ON_CONSENT_SCREEN, String.valueOf(displayOnConsentScreen));
    }

    // Fallback to name if consentScreenText attribute is null
    default String getConsentScreenText() {
        String consentScreenText = getAttribute(CONSENT_SCREEN_TEXT);
        if (ObjectUtil.isBlank(consentScreenText)) {
            consentScreenText = getName();
        }
        return consentScreenText;
    }

    default void setConsentScreenText(String consentScreenText) {
        setAttribute(CONSENT_SCREEN_TEXT, consentScreenText);
    }

    @Override
    default String getGuiOrder() {
        return getAttribute(GUI_ORDER);
    }

    default void setGuiOrder(String guiOrder) {
        setAttribute(GUI_ORDER, guiOrder);
    }

    default boolean isIncludeInTokenScope() {
        String includeInTokenScope = getAttribute(INCLUDE_IN_TOKEN_SCOPE);
        return includeInTokenScope==null ? true : Boolean.parseBoolean(includeInTokenScope);
    }

    default void setIncludeInTokenScope(boolean includeInTokenScope) {
        setAttribute(INCLUDE_IN_TOKEN_SCOPE, String.valueOf(includeInTokenScope));
    }

    default boolean isDynamicScope() {
        return Boolean.parseBoolean(getAttribute(IS_DYNAMIC_SCOPE));
    }

    default void setIsDynamicScope(boolean isDynamicScope) {
        setAttribute(IS_DYNAMIC_SCOPE, String.valueOf(isDynamicScope));
    }

    default String getDynamicScopeRegexp() {
        return getAttribute(DYNAMIC_SCOPE_REGEXP);
    }

    default boolean isHideFromOpenIDProviderMetadata() {
        String hideFromOpenIDProviderMetadata = getAttribute(HIDE_FROM_OPENID_PROVIDER_METADATA);
        return hideFromOpenIDProviderMetadata==null ? false : Boolean.parseBoolean(hideFromOpenIDProviderMetadata);
    }

    default void setHideFromOpenIDProviderMetadata(boolean hideFromOpenIDProviderMetadata) {
        setAttribute(HIDE_FROM_OPENID_PROVIDER_METADATA, String.valueOf(hideFromOpenIDProviderMetadata));
    }
}
