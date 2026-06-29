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
import java.util.Optional;

import org.keycloak.common.Profile;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface ClientScopeModel extends ProtocolMapperContainerModel, ScopeContainerModel, OrderedModel {

    /**
     * The character separator used to specify values when the client scope is parameterized. For instance, {@code <scope>:<value>}.
     */
    String VALUE_SEPARATOR = ":";

    /**
     * Returns true when parameterized scopes are enabled and the scope is defined as parameterized.
     *
     * @param scope The scope to check
     * @return true when the parameterized scopes feature is enabled and the scope is parameterized, false otherwise
     */
    static boolean isParameterizedScope(ClientScopeModel scope) {
        return Profile.isFeatureEnabled(Profile.Feature.PARAMETERIZED_SCOPES) && scope.isParameterizedScope();
    }

    /**
     * @deprecated Use {@link #isParameterizedScope(ClientScopeModel)} instead.
     */
    @Deprecated
    static boolean isDynamicScope(ClientScopeModel scope) {
        return isParameterizedScope(scope);
    }

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
    String IS_PARAMETERIZED_SCOPE = "is.parameterized.scope";
    String PARAMETERIZED_SCOPE_REGEXP = "parameterized.scope.regexp";
    String PARAMETERIZED_SCOPE_TYPE = "parameterized.scope.type";
    String IS_ALWAYS_CONSENT = "always.display.consent";

    /** @deprecated Use {@link #IS_PARAMETERIZED_SCOPE} instead. */
    @Deprecated
    String IS_DYNAMIC_SCOPE = IS_PARAMETERIZED_SCOPE;
    /** @deprecated Use {@link #PARAMETERIZED_SCOPE_REGEXP} instead. */
    @Deprecated
    String DYNAMIC_SCOPE_REGEXP = PARAMETERIZED_SCOPE_REGEXP;
    String INCLUDE_IN_OPENID_PROVIDER_METADATA = "include.in.openid.provider.metadata";

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
            if (isParameterizedScope()) {
                consentScreenText += ": {0}";
            }
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

    default boolean isParameterizedScope() {
        return Boolean.parseBoolean(getAttribute(IS_PARAMETERIZED_SCOPE));
    }

    default boolean isAlwaysConsent() {
        return isParameterizedScope() && isDisplayOnConsentScreen()
                && Boolean.parseBoolean(getAttribute(IS_ALWAYS_CONSENT))
                && Profile.isFeatureEnabled(Profile.Feature.PARAMETERIZED_SCOPES);
    }

    default void setIsParameterizedScope(boolean isParameterizedScope) {
        setAttribute(IS_PARAMETERIZED_SCOPE, String.valueOf(isParameterizedScope));
    }

    default String getParameterizedScopeRegexp() {
        return getAttribute(PARAMETERIZED_SCOPE_REGEXP);
    }

    /**
     * Extracts the parameter value from a requested scope string in the format {@code scopeName:parameterValue}.
     *
     * @param requestScope the requested scope string, e.g. {@code "my_scope:some_value"}
     * @return the extracted parameter value, or empty if this is not a parameterized scope or no valid parameter is present
     */
    default Optional<String> getParameterFromScope(String requestScope) {
        if (!isParameterizedScope() || StringUtil.isBlank(requestScope)) {
            return Optional.empty();
        }
        String prefix = getName() + VALUE_SEPARATOR;
        if (!requestScope.startsWith(prefix)) {
            return Optional.empty();
        }
        String value = requestScope.substring(prefix.length()).trim();
        return Optional.of(value).filter(v -> !v.isEmpty());
    }

    /** @deprecated Use {@link #isParameterizedScope()} instead. */
    @Deprecated
    default boolean isDynamicScope() {
        return isParameterizedScope();
    }

    /** @deprecated Use {@link #setIsParameterizedScope(boolean)} instead. */
    @Deprecated
    default void setIsDynamicScope(boolean isDynamicScope) {
        setIsParameterizedScope(isDynamicScope);
    }

    /** @deprecated Use {@link #getParameterizedScopeRegexp()} instead. */
    @Deprecated
    default String getDynamicScopeRegexp() {
        return getParameterizedScopeRegexp();
    }

    default boolean isIncludeInOpenIDProviderMetadata() {
        String includeInOpenIDProviderMetadata = getAttribute(INCLUDE_IN_OPENID_PROVIDER_METADATA);
        return includeInOpenIDProviderMetadata == null ? true : Boolean.parseBoolean(includeInOpenIDProviderMetadata);
    }

    default void setIncludeInOpenIDProviderMetadata(boolean includeInOpenIDProviderMetadata) {
        setAttribute(INCLUDE_IN_OPENID_PROVIDER_METADATA, String.valueOf(includeInOpenIDProviderMetadata));
    }
}
