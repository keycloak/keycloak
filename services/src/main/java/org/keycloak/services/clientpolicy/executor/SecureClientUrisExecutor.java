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

package org.keycloak.services.clientpolicy.executor;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientRegisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AuthorizationRequestContext;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;
import org.keycloak.services.clientpolicy.context.DynamicClientRegisterContext;
import org.keycloak.services.clientpolicy.context.DynamicClientUpdateContext;
import org.keycloak.validation.ClientUriField;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureClientUrisExecutor implements ClientPolicyExecutorProvider<SecureClientUrisExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureClientUrisExecutor.class);

    private final KeycloakSession session;
    private Configuration configuration;


    public SecureClientUrisExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(Configuration config) {
        this.configuration = config;
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    @Override
    public String getProviderId() {
        return SecureClientUrisExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
                if (context instanceof AdminClientRegisterContext || context instanceof DynamicClientRegisterContext) {
                    ClientRepresentation clientRep = ((ClientCRUDContext)context).getProposedClientRepresentation();
                    confirmSecureUris(clientRep);

                    // Use rootUrl as default redirectUrl to avoid creation of redirectUris with wildcards, which is done at later stages during client creation
                    if (clientRep.getRootUrl() != null && (clientRep.getRedirectUris() == null || clientRep.getRedirectUris().isEmpty())) {
                        logger.debugf("Setup Redirect URI = %s for client %s", clientRep.getRootUrl(), clientRep.getClientId());
                        clientRep.setRedirectUris(Collections.singletonList(clientRep.getRootUrl()));
                    }
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case UPDATE:
                if (context instanceof AdminClientUpdateContext || context instanceof DynamicClientUpdateContext) {
                    confirmSecureUris(((ClientCRUDContext) context).getProposedClientRepresentation());
                } else {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "not allowed input format.");
                }
                return;
            case AUTHORIZATION_REQUEST:
                confirmSecureRedirectUri(((AuthorizationRequestContext)context).getRedirectUri());
                return;
            default:
        }
    }

    private void confirmSecureUris(ClientRepresentation clientRep) throws ClientPolicyException {
        ClientUriField.validate(clientRep, (field, values) -> {
            if (field == ClientUriField.WEB_ORIGINS || field == ClientUriField.POST_LOGOUT_REDIRECT_URIS) {
                if (field == ClientUriField.POST_LOGOUT_REDIRECT_URIS) {
                    values = values.stream().filter(value -> !"-".equals(value)).toList();
                }
                values = List.copyOf(RedirectUtils.resolveUrlsWithRedirects(session, values, clientRep.getRootUrl(),
                        clientRep.getRedirectUris(), field == ClientUriField.WEB_ORIGINS));
            }
            String errorName = switch (field) {
                case BACKCHANNEL_LOGOUT_URL -> "logoutUrl";
                case POST_LOGOUT_REDIRECT_URIS -> "postLogoutUris";
                default -> field.getName();
            };
            confirmSecureUris(values, errorName);
        });
    }

    private void confirmSecureUris(List<String> uris, String uriType) throws ClientPolicyException {
        if (uris == null || uris.isEmpty()) {
            return;
        }

        for (String uri : uris) {
            if (!uri.isEmpty()) {
                logger.tracev("{0} = {1}", uriType, uri);
                if (!uri.startsWith("https://") || uri.contains("*")) {
                    throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT_METADATA, "Invalid " + uriType);
                }
            }
        }
    }

    private void confirmSecureRedirectUri(String redirectUri) throws ClientPolicyException {
        if (redirectUri == null || redirectUri.isEmpty()) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "no redirect_uri specified.");
        }

        logger.tracev("Redirect URI = {0}", redirectUri);
        if (redirectUri.contains("*")) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid redirect_uri");
        }
        if (!redirectUri.startsWith("https://")) {
            if (!configuration.allowHttpOnLocalhost || !isHttpLoopbackUri(redirectUri)) {
                throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid redirect_uri");
            }
        }
    }

    private static boolean isHttpLoopbackUri(String redirectUri) {
        URI uri;
        try {
            uri = new URI(redirectUri);
        } catch (URISyntaxException e) {
            logger.debugv("Invalid redirect_uri syntax: {0}", redirectUri);
            return false;
        }

        if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            return false;
        }

        return RedirectUtils.LOOPBACK_INTERFACES.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {

        @JsonProperty("allow-http-on-localhost")
        protected boolean allowHttpOnLocalhost;

        public boolean getAllowHttpOnLocalhost() {
            return allowHttpOnLocalhost;
        }

        public void setAllowHttpOnLocalhost(boolean allowHttpOnLocalhost) {
            this.allowHttpOnLocalhost = allowHttpOnLocalhost;
        }
    }
}
