/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.authentication.authenticators.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.util.Time;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCProviderConfig;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.Urls;

/**
 * Common validation for JWT client authentication with private_key_jwt or with client_secret
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWTClientValidator {

    private static final Logger logger = Logger.getLogger(JWTClientValidator.class);

    private final ClientAuthenticationFlowContext context;
    private final RealmModel realm;
    private final int currentTime;
    private final String clientAuthenticatorProviderId;

    private MultivaluedMap<String, String> params;
    private String clientAssertion;
    private JWSInput jws;
    private JsonWebToken token;
    private ClientModel client;

    private static final int ALLOWED_CLOCK_SKEW = 15; // sec

    public JWTClientValidator(ClientAuthenticationFlowContext context, String clientAuthenticatorProviderId) {
        this.context = context;
        this.realm = context.getRealm();
        this.currentTime = Time.currentTime();
        this.clientAuthenticatorProviderId = clientAuthenticatorProviderId;
    }

    public boolean clientAssertionParametersValidation() {
        //KEYCLOAK-19461: Needed for quarkus resteasy implementation throws exception when called with mediaType authentication/json in OpenShiftTokenReviewEndpoint
        if(!isFormDataRequest(context.getHttpRequest())) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Parameter client_assertion_type is missing");
            context.challenge(challengeResponse);
            return false;
        }

        params = context.getHttpRequest().getDecodedFormParameters();

        String clientAssertionType = params.getFirst(OAuth2Constants.CLIENT_ASSERTION_TYPE);
        clientAssertion = params.getFirst(OAuth2Constants.CLIENT_ASSERTION);

        if (clientAssertionType == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Parameter client_assertion_type is missing");
            context.challenge(challengeResponse);
            return false;
        }

        if (!clientAssertionType.equals(OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT)) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Parameter client_assertion_type has value '"
                    + clientAssertionType + "' but expected is '" + OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT + "'");
            context.challenge(challengeResponse);
            return false;
        }

        if (clientAssertion == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "client_assertion parameter missing");
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
            return false;
        }

        return true;
    }

    public void readJws() throws JWSInputException {
        if (clientAssertion == null) throw new IllegalStateException("Incorrect usage. Variable 'clientAssertion' is null. Need to validate clientAssertion first before read JWS");

        jws = new JWSInput(clientAssertion);
        token = jws.readJsonContent(JsonWebToken.class);
    }

    public boolean validateClient() {
        if (token == null) throw new IllegalStateException("Incorrect usage. Variable 'token' is null. Need to read JWS first before validateClient");

        String clientId = token.getSubject();
        if (clientId == null) {
            throw new RuntimeException("Can't identify client. Subject missing on JWT token");
        }

        if (!clientId.equals(token.getIssuer())) {
            throw new RuntimeException("Issuer mismatch. The issuer should match the subject");
        }

        String clientIdParam = params.getFirst(OAuth2Constants.CLIENT_ID);
        if (clientIdParam != null && !clientIdParam.equals(clientId)) {
            throw new RuntimeException("client_id parameter not matching with client from JWT token");
        }

        context.getEvent().client(clientId);
        client = realm.getClientByClientId(clientId);
        if (client == null) {
            context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
            return false;
        } else {
            context.setClient(client);
        }

        if (!client.isEnabled()) {
            context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
            return false;
        }

        if (!clientAuthenticatorProviderId.equals(client.getClientAuthenticatorType())) {
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, null);
            return false;
        }

        return true;
    }

    public boolean validateSignatureAlgorithm() {
        if (jws == null) throw new IllegalStateException("Incorrect usage. Variable 'jws' is null. Need to read token first before validate signature algorithm");
        if (client == null) throw new IllegalStateException("Incorrect usage. Variable 'client' is null. Need to validate client first before validate signature algorithm");

        String expectedSignatureAlg = OIDCAdvancedConfigWrapper.fromClientModel(client).getTokenEndpointAuthSigningAlg();
        if (jws.getHeader().getAlgorithm() == null || jws.getHeader().getAlgorithm().name() == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "invalid signature algorithm");
            context.challenge(challengeResponse);
            return false;
        }

        String actualSignatureAlg = jws.getHeader().getAlgorithm().name();
        if (expectedSignatureAlg != null && !expectedSignatureAlg.equals(actualSignatureAlg)) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "invalid signature algorithm");
            context.challenge(challengeResponse);
            return false;
        }

        return true;
    }

    public void validateToken() {
        if (token == null) throw new IllegalStateException("Incorrect usage. Variable 'token' is null. Need to read token first before validateToken");

        if (!token.isActive(ALLOWED_CLOCK_SKEW)) {
            throw new RuntimeException("Token is not active");
        }

        if ((token.getExp() == null || token.getExp() <= 0) && (token.getIat() == null || token.getIat() <= 0)) {
            throw new RuntimeException("Token cannot be validated. Neither the exp nor the iat claim are present.");
        }

        // KEYCLOAK-2986, token-timeout or token-expiration in keycloak.json might not be used
        if (token.getExp() == null || token.getExp() <= 0) { // in case of "exp" not exist
            if (token.getIat() + ALLOWED_CLOCK_SKEW + 10 < currentTime) { // consider "exp" = 10, client's clock delays from Keycloak's clock
                throw new RuntimeException("Token is not active");
            }
        } else {
            if ((token.getIat() != null && token.getIat() > 0) && token.getIat() - ALLOWED_CLOCK_SKEW > currentTime) { // consider client's clock is ahead from Keycloak's clock
                throw new RuntimeException("Token was issued in the future");
            }
        }

        if (token.getId() == null) {
            throw new RuntimeException("Missing ID on the token");
        }
    }

    private long calculateLifespanInSeconds()  {
        if ((token.getExp() == null || token.getExp() <= 0) && (token.getIat() == null || token.getIat() <= 0)) {
            throw new RuntimeException("Token cannot be validated. Neither the exp nor the iat claim are present.");
        }

        // rfc7523 marks exp as required and iat as optional: https://datatracker.ietf.org/doc/html/rfc7523#section-3
        if (token.getExp() == null || token.getExp() <= 0) {
            // exp not present but iat present, just allow a short period of time from iat (10s)
            final long lifespan = token.getIat() + ALLOWED_CLOCK_SKEW + 10 - currentTime;
            if (lifespan <= 0) {
                throw new RuntimeException("Token is not active");
            }
            return lifespan;
        } else if (token.getIat() == null || token.getIat() <= 0) {
            // iat not present but exp present, the max-exp should not be exceeded
            final int maxExp = OIDCAdvancedConfigWrapper.fromClientModel(client).getTokenEndpointAuthSigningMaxExp();
            final long lifespan = token.getExp() - currentTime;
            if (lifespan > maxExp) {
                throw new RuntimeException("Token expiration is too far in the future and iat claim not present in token");
            }
            return lifespan;
        } else {
            // both iat and exp present, the token is just allowed to be used max-age as much
            if (token.getIat() - ALLOWED_CLOCK_SKEW > currentTime) {
                throw new RuntimeException("Token was issued in the future");
            }
            final int maxExp = OIDCAdvancedConfigWrapper.fromClientModel(client).getTokenEndpointAuthSigningMaxExp();
            final long lifespan = Math.min(token.getExp() - currentTime, maxExp);
            if (lifespan <= 0) {
                throw new RuntimeException("Token is not active");
            }
            if (currentTime > token.getIat() + maxExp) {
                throw new RuntimeException("Token was issued too far in the past to be used now");
            }
            return lifespan;
        }
    }

    public void validateTokenReuse() {
        if (token == null) throw new IllegalStateException("Incorrect usage. Variable 'token' is null. Need to read token first before validateToken reuse");
        if (client == null) throw new IllegalStateException("Incorrect usage. Variable 'client' is null. Need to validate client first before validateToken reuse");

        SingleUseObjectProvider singleUseCache = context.getSession().singleUseObjects();
        long lifespanInSecs = calculateLifespanInSeconds();
        if (singleUseCache.putIfAbsent(token.getId(), lifespanInSecs)) {
            logger.tracef("Added token '%s' to single-use cache. Lifespan: %d seconds, client: %s", token.getId(), lifespanInSecs, client.getClientId());

        } else {
            logger.warnf("Token '%s' already used when authenticating client '%s'.", token.getId(), client.getClientId());
            throw new RuntimeException("Token reuse detected");
        }
    }

    public void validateTokenAudience(ClientAuthenticationFlowContext context, RealmModel realm, JsonWebToken token) {
        List<String> expectedAudiences = getExpectedAudiences(context, realm);
        if (!token.hasAnyAudience(expectedAudiences)) {
            throw new RuntimeException("Token audience doesn't match domain. Expected audiences are any of " + expectedAudiences
                    + " but audience from token is '" + Arrays.asList(token.getAudience()) + "'");
        }

        if (!isAllowMultipleAudiencesForJwtClientAuthentication(context) && token.getAudience().length > 1) {
            throw new RuntimeException("Multiple audiences not allowed in the JWT token for client authentication");
        }
    }

    private boolean isAllowMultipleAudiencesForJwtClientAuthentication(ClientAuthenticationFlowContext context) {
        OIDCLoginProtocol loginProtocol = (OIDCLoginProtocol) context.getSession().getProvider(LoginProtocol.class, OIDCLoginProtocol.LOGIN_PROTOCOL);
        OIDCProviderConfig config = loginProtocol.getConfig();
        return config.isAllowMultipleAudiencesForJwtClientAuthentication();
    }

    private List<String> getExpectedAudiences(ClientAuthenticationFlowContext context, RealmModel realm) {

        String issuerUrl = Urls.realmIssuer(context.getUriInfo().getBaseUri(), realm.getName());
        String tokenUrl = OIDCLoginProtocolService.tokenUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        String tokenIntrospectUrl = OIDCLoginProtocolService.tokenIntrospectionUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        String parEndpointUrl = ParEndpoint.parUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        List<String> expectedAudiences = new ArrayList<>(Arrays.asList(issuerUrl, tokenUrl, tokenIntrospectUrl, parEndpointUrl));
        String backchannelAuthenticationUrl = CibaGrantType.authorizationUrl(context.getUriInfo().getBaseUriBuilder()).build(realm.getName()).toString();
        expectedAudiences.add(backchannelAuthenticationUrl);

        return expectedAudiences;
    }

    public ClientAuthenticationFlowContext getContext() {
        return context;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public MultivaluedMap<String, String> getParams() {
        return params;
    }

    public String getClientAssertion() {
        return clientAssertion;
    }

    public JWSInput getJws() {
        return jws;
    }

    public JsonWebToken getToken() {
        return token;
    }

    public ClientModel getClient() {
        return client;
    }

    private boolean isFormDataRequest(HttpRequest request) {
        MediaType mediaType = request.getHttpHeaders().getMediaType();
        return mediaType != null && mediaType.isCompatible(MediaType.APPLICATION_FORM_URLENCODED_TYPE);
    }
}
