/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.executor;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisteredContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.SamlAuthnRequestContext;
import org.keycloak.services.clientpolicy.context.SamlLogoutRequestContext;
import org.keycloak.utils.StringUtil;
import org.keycloak.validation.ClientUriField;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author rmartinc
 */
public class SamlSecureClientUrisExecutor implements ClientPolicyExecutorProvider<SamlSecureClientUrisExecutor.Configuration> {

    private final KeycloakSession session;
    private Configuration config;

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("allow-wildcard-redirects")
        protected boolean allowWildcardRedirects;

        public Configuration() {
            this(false);
        }

        public Configuration(boolean allowWildcardRedirects) {
            this.allowWildcardRedirects = allowWildcardRedirects;
        }

        public boolean isAllowWildcardResirects() {
            return allowWildcardRedirects;
        }

        public void setAllowWildcardResirects(boolean allowWildcardRedirects) {
            this.allowWildcardRedirects = allowWildcardRedirects;
        }
    }

    public SamlSecureClientUrisExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.config = config;
    }

    @Override
    public String getProviderId() {
        return SamlSecureClientUrisExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTERED -> {
                confirmSecureUris(((AdminClientRegisteredContext)context).getTargetClient(), OAuthErrorException.INVALID_CLIENT_METADATA);
            }
            case UPDATED -> {
                confirmSecureUris(((AdminClientUpdatedContext)context).getTargetClient(), OAuthErrorException.INVALID_CLIENT_METADATA);
            }
            case SAML_AUTHN_REQUEST -> {
                confirmLoginRedirectUri((SamlAuthnRequestContext) context);
            }
            case SAML_LOGOUT_REQUEST -> {
                confirmLogoutRedirectUri((SamlLogoutRequestContext) context);
            }
        }
    }

    private void confirmLoginRedirectUri(SamlAuthnRequestContext context) throws ClientPolicyException {
        AuthnRequestType request = context.getRequest();
        URI uri = request.getAssertionConsumerServiceURL();
        if (uri != null) {
            confirmSecureUri(uri.toString(), "AssertionConsumerServiceURL", OAuthErrorException.INVALID_REQUEST);
        }
        confirmSecureUris(context.getClient(), OAuthErrorException.INVALID_REQUEST);
    }

    private void confirmLogoutRedirectUri(SamlLogoutRequestContext context) throws ClientPolicyException {
        confirmSecureUris(context.getClient(), OAuthErrorException.INVALID_REQUEST);
    }

    private String getUriType(ClientUriField field) {
        return switch (field) {
            case ROOT_URL -> "Root URL";
            case ADMIN_URL -> "Master SAML Processing URL";
            case BASE_URL -> "Home URL";
            case REDIRECT_URIS -> "Valid redirect URIs";
            case SAML_ASSERTION_CONSUMER_URL_POST_URI -> "Assertion Consumer Service POST Binding URL";
            case SAML_ASSERTION_CONSUMER_URL_REDIRECT_URI -> "Assertion Consumer Service Redirect Binding URL";
            case SAML_ASSERTION_CONSUMER_URL_ARTIFACT_URI -> "Artifact Binding URL";
            case SAML_SINGLE_LOGOUT_SERVICE_URL_POST_URI -> "Logout Service POST Binding URL";
            case SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_URI -> "Logout Service ARTIFACT Binding URL";
            case SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_URI -> "Logout Service Redirect Binding URL";
            case SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_URI -> "Logout Service SOAP Binding URL";
            case SAML_ARTIFACT_RESOLUTION_SERVICE_URL_URI -> "Artifact Resolution Service";
            case SAML_METADATA_DESCRIPTOR_URL -> "Metadata descriptor URL";
            default -> field.getName();
        };
    }

    private void confirmSecureUri(String uri, String uriType, String error) throws ClientPolicyException {
        if (StringUtil.isBlank(uri)) {
            return;
        }

        if (!uri.startsWith("https:")) { // make this configurable? (allowed schemes...)
            throw new ClientPolicyException(error, "Non secure scheme for " + uriType);
        }
    }

    private void confirmNoWildcard(String uri, String uriType, String error) throws ClientPolicyException {
        if (uri.endsWith("*") && !uri.contains("?") && !uri.contains("#")) {
            throw new ClientPolicyException(error,
                    "Unsecure wildcard redirect " + uri + " for " + uriType);
        }
    }

    private void confirmSecureUris(ClientModel client, String error) throws ClientPolicyException {
        if (!SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            return;
        }

        ClientUriField.validate(client, (field, values) -> {
            if (field == ClientUriField.REDIRECT_URIS) {
                values = List.copyOf(RedirectUtils.resolveValidRedirects(session, client.getRootUrl(), new LinkedHashSet<>(values)));
            } else if (field == ClientUriField.WEB_ORIGINS || field == ClientUriField.POST_LOGOUT_REDIRECT_URIS) {
                if (field == ClientUriField.POST_LOGOUT_REDIRECT_URIS) {
                    values = values.stream().filter(value -> !"-".equals(value)).toList();
                }
                values = List.copyOf(RedirectUtils.resolveUrlsWithRedirects(session, values, client.getRootUrl(),
                        ClientUriField.REDIRECT_URIS.getValues(client), field == ClientUriField.WEB_ORIGINS));
            }
            String uriType = getUriType(field);
            for (String value : values) {
                confirmSecureUri(value, uriType, error);
                if (field == ClientUriField.REDIRECT_URIS && !config.isAllowWildcardResirects()) {
                    confirmNoWildcard(value, uriType, error);
                }
            }
        });
    }
}
