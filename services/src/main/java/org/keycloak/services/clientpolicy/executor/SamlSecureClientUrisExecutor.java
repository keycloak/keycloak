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
import java.util.Collection;
import java.util.Map;

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
                confirmSecureUris(((AdminClientRegisteredContext)context).getTargetClient());
            }
            case UPDATED -> {
                confirmSecureUris(((AdminClientUpdatedContext)context).getTargetClient());
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
        } else {
            // use configuration for login, check URLs for login are all secure
            ClientModel client = context.getClient();
            confirmSecureUri(client.getManagementUrl(),
                    "Master SAML Processing URL", OAuthErrorException.INVALID_REQUEST);
            confirmSecureUri(client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE),
                    "Assertion Consumer Service POST Binding URL", OAuthErrorException.INVALID_REQUEST);
            confirmSecureUri(client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE),
                    "Assertion Consumer Service Redirect Binding URL", OAuthErrorException.INVALID_REQUEST);
            confirmSecureUri(client.getAttribute(SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE),
                    "Artifact Binding URL", OAuthErrorException.INVALID_REQUEST);
        }
    }

    private void confirmLogoutRedirectUri(SamlLogoutRequestContext context) throws ClientPolicyException {
        // check logout URLs are all secure
        ClientModel client = context.getClient();
        confirmSecureUri(client.getManagementUrl(),
                "Master SAML Processing URL", OAuthErrorException.INVALID_REQUEST);
        confirmSecureUri(client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE),
                "Logout Service POST Binding URL", OAuthErrorException.INVALID_REQUEST);
        confirmSecureUri(client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE),
                "Logout Service ARTIFACT Binding URL", OAuthErrorException.INVALID_REQUEST);
        confirmSecureUri(client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE),
                "Logout Service Redirect Binding URL", OAuthErrorException.INVALID_REQUEST);
        confirmSecureUri(client.getAttribute(SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE),
                "Logout Service SOAP Binding URL", OAuthErrorException.INVALID_REQUEST);
    }

    private void confirmSecureUri(String uri, String uriType) throws ClientPolicyException {
        confirmSecureUri(uri, uriType, OAuthErrorException.INVALID_CLIENT_METADATA);
    }

    private void confirmSecureUri(String uri, String uriType, String error) throws ClientPolicyException {
        if (StringUtil.isBlank(uri)) {
            return;
        }

        if (!uri.startsWith("https:")) { // make this configurable? (allowed schemes...)
            throw new ClientPolicyException(error, "Non secure scheme for " + uriType);
        }
    }

    private void confirmNoWildcard(String uri, String uriType) throws ClientPolicyException {
        if (uri.endsWith("*") && !uri.contains("?") && !uri.contains("#")) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA,
                    "Unsecure wildcard redirect " + uri + " for " + uriType);
        }
    }

    private void confirmRedirectUris(Collection<String> uris, String uriType) throws ClientPolicyException {
        if (uris == null) {
            return;
        }

        for (String uri : uris) {
            confirmSecureUri(uri, uriType);
            if (!config.isAllowWildcardResirects()) {
                confirmNoWildcard(uri, uriType);
            }
        }
    }

    private void confirmSecureUris(ClientModel client) throws ClientPolicyException {
        if (!SamlProtocol.LOGIN_PROTOCOL.equals(client.getProtocol())) {
            return;
        }

        confirmSecureUri(client.getRootUrl(), "Root URL");
        confirmSecureUri(client.getManagementUrl(), "Master SAML Processing URL");
        confirmSecureUri(client.getBaseUrl(), "Home URL");

        confirmRedirectUris(RedirectUtils.resolveValidRedirects(session, client.getRootUrl(), client.getRedirectUris()),
                "Valid redirect URIs");

        Map<String, String> attrs = Map.of(
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE, "Assertion Consumer Service POST Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_REDIRECT_ATTRIBUTE, "Assertion Consumer Service Redirect Binding URL",
                SamlProtocol.SAML_ASSERTION_CONSUMER_URL_ARTIFACT_ATTRIBUTE, "Artifact Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_POST_ATTRIBUTE, "Logout Service POST Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_ARTIFACT_ATTRIBUTE, "Logout Service ARTIFACT Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_REDIRECT_ATTRIBUTE, "Logout Service Redirect Binding URL",
                SamlProtocol.SAML_SINGLE_LOGOUT_SERVICE_URL_SOAP_ATTRIBUTE, "Logout Service SOAP Binding URL",
                SamlProtocol.SAML_ARTIFACT_RESOLUTION_SERVICE_URL_ATTRIBUTE, "Artifact Resolution Service"
        );

        if (client.getAttributes() != null) {
            for (Map.Entry<String, String> attr : attrs.entrySet()) {
                confirmSecureUri(client.getAttribute(attr.getKey()), attr.getValue());
            }
        }
    }
}
