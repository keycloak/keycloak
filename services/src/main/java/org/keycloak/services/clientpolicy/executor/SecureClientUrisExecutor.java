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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
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

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureClientUrisExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    private static final Logger logger = Logger.getLogger(SecureClientUrisExecutor.class);

    private final KeycloakSession session;

    public SecureClientUrisExecutor(KeycloakSession session) {
        this.session = session;
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
                    confirmSecureUris(((ClientCRUDContext)context).getProposedClientRepresentation());
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
        // rootUrl
        String rootUrl = clientRep.getRootUrl();
        if (rootUrl != null) confirmSecureUris(List.of(rootUrl), "rootUrl");

        // adminUrl
        String adminUrl = clientRep.getAdminUrl();
        if (adminUrl != null) confirmSecureUris(List.of(adminUrl), "adminUrl");

        // baseUrl
        String baseUrl = clientRep.getBaseUrl();
        if (baseUrl != null) confirmSecureUris(List.of(baseUrl), "baseUrl");

        // backchannel logout URL
        String logoutUrl = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL);
        if (logoutUrl != null) confirmSecureUris(List.of(logoutUrl), "logoutUrl");

        // OAuth2 : redirectUris
        List<String> redirectUris = clientRep.getRedirectUris();
        if (redirectUris != null) confirmSecureUris(redirectUris, "redirectUris");

        // web origins
        List<String> webOrigins = clientRep.getWebOrigins();
        if (webOrigins != null) {
            List<String> resolvedWebOriginUrls = resolveUrlWithRedirects(webOrigins, redirectUris, rootUrl, true);
            confirmSecureUris(resolvedWebOriginUrls, "webOrigins");
        }

        // OAuth2 : jwks_uri
        String jwksUri = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(OIDCConfigAttributes.JWKS_URL);
        if (jwksUri != null) confirmSecureUris(List.of(jwksUri), "jwksUri");

        // OIDD : requestUris
        List<String> requestUris = getAttributeMultivalued(clientRep, OIDCConfigAttributes.REQUEST_URIS);
        if (requestUris != null) confirmSecureUris(requestUris, "requestUris");

        // CIBA : client notification endpoint
        String clientNotificationEndpoint = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT);
        if (clientNotificationEndpoint != null) confirmSecureUris(List.of(clientNotificationEndpoint), "cibaClientNotificationEndpoint");

        // OIDC: Post Logout URL
        List<String> postLogoutRedirectUris = getAttributeMultivalued(clientRep, OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS);
        if (postLogoutRedirectUris != null && !postLogoutRedirectUris.isEmpty()) {
            List<String> validRedirects = clientRep.getRedirectUris() != null ? clientRep.getRedirectUris() : Collections.emptyList();
            List<String> resolvedPostLogoutUrls = resolveUrlWithRedirects(postLogoutRedirectUris, validRedirects, clientRep.getRootUrl(), false);
            confirmSecureUris(resolvedPostLogoutUrls, "postLogoutUris");
        }

        // logoUri
        String logoUri = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(OIDCConfigAttributes.LOGO_URI);
        if (logoUri != null) confirmSecureUris(List.of(logoUri), "logoUri");

        // termsOfServiceUri
        String termsOfServiceUri = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(OIDCConfigAttributes.TOS_URI);
        if (termsOfServiceUri != null) confirmSecureUris(List.of(termsOfServiceUri), "tosUri");

        // policyUri
        String policyUri = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(OIDCConfigAttributes.POLICY_URI);
        if (policyUri != null) confirmSecureUris(List.of(policyUri), "policyUri");
    }

    private List<String> getAttributeMultivalued(ClientRepresentation clientRep, String attrKey) {
        String attrValue = Optional.ofNullable(clientRep.getAttributes()).orElse(Collections.emptyMap()).get(attrKey);
        if (attrValue == null) return Collections.emptyList();
        return Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(attrValue));
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
        if (!redirectUri.startsWith("https://") || redirectUri.contains("*")) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid redirect_uri");
        }
    }

    private List<String> resolveUrlWithRedirects(List<String> originalUrls, List<String> redirectUris,
                                                 String rootUrl, boolean returnAsOrigins) {
        if (originalUrls == null || originalUrls.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> resolvedUrls = RedirectUtils.resolveUrlsWithRedirects(session, originalUrls, rootUrl, redirectUris, returnAsOrigins);
        return new ArrayList<>(resolvedUrls);
    }
}
