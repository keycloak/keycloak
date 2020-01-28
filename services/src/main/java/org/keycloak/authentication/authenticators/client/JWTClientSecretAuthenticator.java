/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.authenticators.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.SingleUseTokenStoreProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;

/**
 * Client authentication based on JWT signed by client secret instead of private key .
 * See <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">specs</a> for more details.
 *
 * This is server side, which verifies JWT from client_assertion parameter, where the assertion was created on adapter side by
 * org.keycloak.adapters.authentication.JWTClientSecretCredentialsProvider
 *
 * TODO: Try to create abstract superclass to be shared with {@link JWTClientAuthenticator}. Most of the code can be reused
 *
 */
public class JWTClientSecretAuthenticator extends AbstractClientAuthenticator {

    private static final Logger logger = Logger.getLogger(JWTClientSecretAuthenticator.class);

    public static final String PROVIDER_ID = "client-secret-jwt";

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

        String clientAssertionType = params.getFirst(OAuth2Constants.CLIENT_ASSERTION_TYPE);
        String clientAssertion = params.getFirst(OAuth2Constants.CLIENT_ASSERTION);

        if (clientAssertionType == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Parameter client_assertion_type is missing");
            context.challenge(challengeResponse);
            return;
        }

        if (!clientAssertionType.equals(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT)) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Parameter client_assertion_type has value '"
                    + clientAssertionType + "' but expected is '" + OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT + "'");
            context.challenge(challengeResponse);
            return;
        }

        if (clientAssertion == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "client_assertion parameter missing");
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
            return;
        }

        try {
            JWSInput jws = new JWSInput(clientAssertion);
            JsonWebToken token = jws.readJsonContent(JsonWebToken.class);

            RealmModel realm = context.getRealm();
            String clientId = token.getSubject();
            if (clientId == null) {
                throw new RuntimeException("Can't identify client. Issuer missing on JWT token");
            }

            context.getEvent().client(clientId);
            ClientModel client = realm.getClientByClientId(clientId);
            if (client == null) {
                context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
                return;
            } else {
                context.setClient(client);
            }

            if (!client.isEnabled()) {
                context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
                return;
            }

            String clientSecretString = client.getSecret();
            if (clientSecretString == null) {
                context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, null);
                return;
            }

            boolean signatureValid;
            try {
                JsonWebToken jwt = context.getSession().tokens().decodeClientJWT(clientAssertion, client, JsonWebToken.class);
                signatureValid = jwt != null;
            } catch (RuntimeException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                throw new RuntimeException("Signature on JWT token by client secret failed validation", cause);
            }
            if (!signatureValid) {
                throw new RuntimeException("Signature on JWT token by client secret  failed validation");
            }
            // According to <a href="http://openid.net/specs/openid-connect-core-1_0.html#ClientAuthentication">OIDC's client authentication spec</a>,
            // JWT contents and verification in client_secret_jwt is the same as in private_key_jwt

            // Allow both "issuer" or "token-endpoint" as audience
            String issuerUrl = Urls.realmIssuer(context.getUriInfo().getBaseUri(), realm.getName());
            String tokenUrl = OIDCLoginProtocolService.tokenUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
            if (!token.hasAudience(issuerUrl) && !token.hasAudience(tokenUrl)) {
                throw new RuntimeException("Token audience doesn't match domain. Realm issuer is '" + issuerUrl + "' but audience from token is '" + Arrays.asList(token.getAudience()).toString() + "'");
            }

            if (!token.isActive()) {
                throw new RuntimeException("Token is not active");
            }

            // KEYCLOAK-2986, token-timeout or token-expiration in keycloak.json might not be used
            int currentTime = Time.currentTime();
            if (token.getExpiration() == 0 && token.getIssuedAt() + 10 < currentTime) {
                throw new RuntimeException("Token is not active");
            }

            if (token.getId() == null) {
                throw new RuntimeException("Missing ID on the token");
            }

            SingleUseTokenStoreProvider singleUseCache = context.getSession().getProvider(SingleUseTokenStoreProvider.class);
            int lifespanInSecs = Math.max(token.getExpiration() - currentTime, 10);
            if (singleUseCache.putIfAbsent(token.getId(), lifespanInSecs)) {

                logger.tracef("Added token '%s' to single-use cache. Lifespan: %d seconds, client: %s", token.getId(), lifespanInSecs, clientId);
            } else {
                logger.warnf("Token '%s' already used when authenticating client '%s'.", token.getId(), clientId);
                throw new RuntimeException("Token reuse detected");
            }

            context.success();
        } catch (Exception e) {
            ServicesLogger.LOGGER.errorValidatingAssertion(e);
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Client authentication with client secret signed JWT failed: " + e.getMessage());
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
        }
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        // This impl doesn't use generic screen in admin console, but has its own screen. So no need to return anything here
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        // e.g. client adapter's keycloak.json
        // "credentials": {
        //   "secret-jwt": {
        //     "secret": "234234-234234-234234",
        //     "algorithm": "HS256"
        //   }
        // }
        Map<String, Object> props = new HashMap<>();
        props.put("secret", client.getSecret());
        // "algorithm" field is not saved because keycloak does not manage client's property of which algorithm is used for client secret signed JWT.

        Map<String, Object> config = new HashMap<>();
        config.put("secret-jwt", props);
        return config;
    }

    @Override
    public Set<String> getProtocolAuthenticatorMethods(String loginProtocol) {
        if (loginProtocol.equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
            Set<String> results = new HashSet<>();
            results.add(OIDCLoginProtocol.CLIENT_SECRET_JWT);
            return results;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Signed Jwt with Client Secret";
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates client based on signed JWT issued by client and signed with the Client Secret";

    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

}
