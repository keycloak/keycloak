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
 *
 */

package org.keycloak.services.clienttype.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.client.clienttype.ClientType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.delegate.ClientModelLazyDelegate;

/**
 * Delegates to client-type and underlying delegate
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class TypeAwareClientModelDelegate extends ClientModelLazyDelegate {

    private final ClientType clientType;

    public TypeAwareClientModelDelegate(ClientType clientType, Supplier<ClientModel> clientModelSupplier) {
        super(clientModelSupplier);

        if (clientType == null) {
            throw new IllegalArgumentException("Null client type not supported for client " + getClientId());
        }
        this.clientType = clientType;
    }

    @Override
    public boolean isStandardFlowEnabled() {
        return TypedClientSimpleAttribute.STANDARD_FLOW_ENABLED
                .getClientAttribute(clientType, super::isStandardFlowEnabled, Boolean.class);
    }

    @Override
    public void setStandardFlowEnabled(boolean standardFlowEnabled) {
        TypedClientSimpleAttribute.STANDARD_FLOW_ENABLED
                .setClientAttribute(clientType, standardFlowEnabled, super::setStandardFlowEnabled, Boolean.class);
    }

    @Override
    public boolean isBearerOnly() {
        return TypedClientSimpleAttribute.BEARER_ONLY
                .getClientAttribute(clientType, super::isBearerOnly, Boolean.class);
    }

    @Override
    public void setBearerOnly(boolean bearerOnly) {
        TypedClientSimpleAttribute.BEARER_ONLY
                .setClientAttribute(clientType, bearerOnly, super::setBearerOnly, Boolean.class);
    }

    @Override
    public boolean isConsentRequired() {
        return TypedClientSimpleAttribute.CONSENT_REQUIRED
                .getClientAttribute(clientType, super::isConsentRequired, Boolean.class);
    }

    @Override
    public void setConsentRequired(boolean consentRequired) {
        TypedClientSimpleAttribute.CONSENT_REQUIRED
                .setClientAttribute(clientType, consentRequired, super::setConsentRequired, Boolean.class);
    }

    @Override
    public boolean isDirectAccessGrantsEnabled() {
        return TypedClientSimpleAttribute.DIRECT_ACCESS_GRANTS_ENABLED
                .getClientAttribute(clientType, super::isDirectAccessGrantsEnabled, Boolean.class);
    }

    @Override
    public void setDirectAccessGrantsEnabled(boolean directAccessGrantsEnabled) {
        TypedClientSimpleAttribute.DIRECT_ACCESS_GRANTS_ENABLED
                .setClientAttribute(clientType, directAccessGrantsEnabled, super::setDirectAccessGrantsEnabled, Boolean.class);
    }

    @Override
    public boolean isAlwaysDisplayInConsole() {
        return TypedClientSimpleAttribute.ALWAYS_DISPLAY_IN_CONSOLE
                .getClientAttribute(clientType, super::isAlwaysDisplayInConsole, Boolean.class);
    }

    @Override
    public void setAlwaysDisplayInConsole(boolean alwaysDisplayInConsole) {
        TypedClientSimpleAttribute.ALWAYS_DISPLAY_IN_CONSOLE
                .setClientAttribute(clientType, alwaysDisplayInConsole, super::setAlwaysDisplayInConsole, Boolean.class);
    }

    @Override
    public boolean isFrontchannelLogout() {
        return TypedClientSimpleAttribute.FRONTCHANNEL_LOGOUT
                .getClientAttribute(clientType, super::isFrontchannelLogout, Boolean.class);
    }

    @Override
    public void setFrontchannelLogout(boolean frontchannelLogout) {
        TypedClientSimpleAttribute.FRONTCHANNEL_LOGOUT
                .setClientAttribute(clientType, frontchannelLogout, super::setFrontchannelLogout, Boolean.class);
    }

    @Override
    public boolean isImplicitFlowEnabled() {
        return TypedClientSimpleAttribute.IMPLICIT_FLOW_ENABLED
                .getClientAttribute(clientType, super::isImplicitFlowEnabled, Boolean.class);
    }

    @Override
    public void setImplicitFlowEnabled(boolean implicitFlowEnabled) {
        TypedClientSimpleAttribute.IMPLICIT_FLOW_ENABLED
                .setClientAttribute(clientType, implicitFlowEnabled, super::setImplicitFlowEnabled, Boolean.class);
    }

    @Override
    public boolean isServiceAccountsEnabled() {
        return TypedClientSimpleAttribute.SERVICE_ACCOUNTS_ENABLED
                .getClientAttribute(clientType, super::isServiceAccountsEnabled, Boolean.class);
    }

    @Override
    public void setServiceAccountsEnabled(boolean flag) {
        TypedClientSimpleAttribute.SERVICE_ACCOUNTS_ENABLED
                .setClientAttribute(clientType, flag, super::setServiceAccountsEnabled, Boolean.class);
    }

    @Override
    public String getProtocol() {
        return TypedClientSimpleAttribute.PROTOCOL
                .getClientAttribute(clientType, super::getProtocol, String.class);
    }

    @Override
    public void setProtocol(String protocol) {
        TypedClientSimpleAttribute.PROTOCOL
                .setClientAttribute(clientType, protocol, super::setProtocol, String.class);
    }

    @Override
    public boolean isPublicClient() {
        return TypedClientSimpleAttribute.PUBLIC_CLIENT
                .getClientAttribute(clientType, super::isPublicClient, Boolean.class);
    }

    @Override
    public void setPublicClient(boolean flag) {
        TypedClientSimpleAttribute.PUBLIC_CLIENT
                .setClientAttribute(clientType, flag, super::setPublicClient, Boolean.class);
    }

    @Override
    public Set<String> getWebOrigins() {
        return TypedClientSimpleAttribute.WEB_ORIGINS
                .getClientAttribute(clientType, super::getWebOrigins, Set.class);
    }

    @Override
    public void setWebOrigins(Set<String> webOrigins) {
        TypedClientSimpleAttribute.WEB_ORIGINS
                .setClientAttribute(clientType, webOrigins, super::setWebOrigins, Set.class);
    }

    @Override
    public void addWebOrigin(String webOrigin) {
        TypedClientSimpleAttribute.WEB_ORIGINS
                .setClientAttribute(clientType, webOrigin, super::addWebOrigin, String.class);
    }

    @Override
    public void removeWebOrigin(String webOrigin) {
        TypedClientSimpleAttribute.WEB_ORIGINS
                .setClientAttribute(clientType, null, (val) -> super.removeWebOrigin(webOrigin), String.class);
    }

    @Override
    public Set<String> getRedirectUris() {
        return TypedClientSimpleAttribute.REDIRECT_URIS
                .getClientAttribute(clientType, super::getRedirectUris, Set.class);
    }

    @Override
    public void setRedirectUris(Set<String> redirectUris) {
        TypedClientSimpleAttribute.REDIRECT_URIS
                .setClientAttribute(clientType, redirectUris, super::setRedirectUris, Set.class);
    }

    @Override
    public void addRedirectUri(String redirectUri) {
        TypedClientSimpleAttribute.REDIRECT_URIS
                .setClientAttribute(clientType, redirectUri, super::addRedirectUri, String.class);
    }

    @Override
    public void removeRedirectUri(String redirectUri) {
        TypedClientSimpleAttribute.REDIRECT_URIS
            .setClientAttribute(clientType, null, (val) -> super.removeRedirectUri(redirectUri), String.class);
    }

    @Override
    public void setAttribute(String name, String value) {
        TypedClientExtendedAttribute attribute = TypedClientExtendedAttribute.getAttributesByName().get(name);
        if (attribute != null) {
            attribute.setClientAttribute(clientType, value, (newValue) -> super.setAttribute(name, newValue), String.class);
        } else {
            super.setAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        TypedClientExtendedAttribute attribute = TypedClientExtendedAttribute.getAttributesByName().get(name);
        if (attribute != null) {
            attribute.setClientAttribute(clientType, null, (val) -> super.removeAttribute(name), String.class);
        } else {
            super.removeAttribute(name);
        }
    }

    @Override
    public String getAttribute(String name) {
        TypedClientExtendedAttribute attribute = TypedClientExtendedAttribute.getAttributesByName().get(name);
        if (attribute != null) {
            return attribute.getClientAttribute(clientType, () -> super.getAttribute(name), String.class);
        } else {
            return super.getAttribute(name);
        }
    }

    @Override
    public Map<String, String> getAttributes() {
        // Start with attributes set on the delegate.
        Map<String, String> attributes = new HashMap<>(super.getAttributes());

        // Get extended client type attributes and values from the client type configuration.
        Set<String> extendedClientTypeAttributes =
                clientType.getOptionNames().stream()
                .filter(optionName -> TypedClientExtendedAttribute.getAttributesByName().containsKey(optionName))
                .collect(Collectors.toSet());

        // Augment client type attributes on top of attributes on the delegate.
        for (String entry : extendedClientTypeAttributes) {
            attributes.put(entry, getAttribute(entry));
        }

        return attributes;
    }
}
