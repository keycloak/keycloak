/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureClientAuthenticationAssertionExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    private static final Logger logger = Logger.getLogger(SecureClientAuthenticationAssertionExecutor.class);

    private final KeycloakSession session;

    public SecureClientAuthenticationAssertionExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return SecureClientAuthenticationAssertionExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case PUSHED_AUTHORIZATION_REQUEST:
            case TOKEN_REQUEST:
            case TOKEN_REFRESH:
            case TOKEN_INTROSPECT:
            case TOKEN_REVOKE:
                validateClientAssertion();
            default:
        }
    }

    private void validateClientAssertion() throws ClientPolicyException {
        KeycloakContext context = session.getContext();
        ClientModel client = context.getClient();

        // a public client does not need to send its credential for client authentication
        if (client.isPublicClient()) return;

        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();
        String clientAssertionType = params.getFirst(OAuth2Constants.CLIENT_ASSERTION_TYPE);
        String clientAssertion = params.getFirst(OAuth2Constants.CLIENT_ASSERTION);

        if (clientAssertionType == null || clientAssertion == null || !clientAssertionType.equals(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT)) {
            // if the client did not send client assertion, then skit the further validation.
            return;
        }

        // Validate the client assertion format
        String[] parts = clientAssertion.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            logger.warn("client assertion format error");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid client assertion format");
        }

        // Decode the client assertion
        String encodedContent = parts[1];
        byte[] content = Base64Url.decode(encodedContent);
        JsonWebToken token;
        try {
            token = JsonSerialization.readValue(content, JsonWebToken.class);
        } catch (IOException e) {
            logger.warnf("client assertion parse error: %s", e.getMessage());
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid client assertion");
        }

        // Validate the client assertion audience
        String issuerUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName());
        List<String> expectedAudiences = new ArrayList<>(Collections.singletonList(issuerUrl));
        if (token.getAudience() == null || token.getAudience().length != 1) {
            logger.warnf("invalid audience in client assertion - no audience or multiple audiences found");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid audience in client assertion");
        }
        if (!token.hasAnyAudience(expectedAudiences)) {
            logger.warnf("invalid audience in client assertion - audience not issuer URL");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "invalid audience in client assertion");
        }
    }
}
