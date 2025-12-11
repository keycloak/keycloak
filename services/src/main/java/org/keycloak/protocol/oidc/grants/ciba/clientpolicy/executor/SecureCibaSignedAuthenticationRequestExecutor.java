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

package org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.context.BackchannelAuthenticationRequestContext;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequest;
import org.keycloak.protocol.oidc.grants.ciba.endpoints.request.BackchannelAuthenticationEndpointRequestParser;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class SecureCibaSignedAuthenticationRequestExecutor implements ClientPolicyExecutorProvider<SecureCibaSignedAuthenticationRequestExecutor.Configuration> {

    private static final Logger logger = Logger.getLogger(SecureCibaSignedAuthenticationRequestExecutor.class);

    public static final String INVALID_REQUEST_OBJECT = "invalid_request_object";
    public static final Integer DEFAULT_AVAILABLE_PERIOD = Integer.valueOf(3600); // (sec) from FAPI-CIBA requirement

    private final KeycloakSession session;
    private Configuration configuration;

    public SecureCibaSignedAuthenticationRequestExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void setupConfiguration(SecureCibaSignedAuthenticationRequestExecutor.Configuration config) {
        if (config == null) {
            configuration = new Configuration();
            configuration.setAvailablePeriod(DEFAULT_AVAILABLE_PERIOD);
        } else {
            configuration = config;
            if (config.getAvailablePeriod() == null) {
                configuration.setAvailablePeriod(DEFAULT_AVAILABLE_PERIOD);
            }
        }
    }

    @Override
    public Class<Configuration> getExecutorConfigurationClass() {
        return Configuration.class;
    }

    public static class Configuration extends ClientPolicyExecutorConfigurationRepresentation {
        @JsonProperty("available-period")
        protected Integer availablePeriod;

        public Integer getAvailablePeriod() {
            return availablePeriod;
        }

        public void setAvailablePeriod(Integer availablePeriod) {
            this.availablePeriod = availablePeriod;
        }

    }

    @Override
    public String getProviderId() {
        return SecureCibaSignedAuthenticationRequestExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case BACKCHANNEL_AUTHENTICATION_REQUEST:
                BackchannelAuthenticationRequestContext backchannelAuthenticationRequestContext = (BackchannelAuthenticationRequestContext)context;
                executeOnBackchannelAuthenticationRequest(backchannelAuthenticationRequestContext.getRequest(),
                    backchannelAuthenticationRequestContext.getRequestParameters());
                return;
            default:
                return;
        }
    }

    private void executeOnBackchannelAuthenticationRequest(
            BackchannelAuthenticationEndpointRequest request,
            MultivaluedMap<String, String> params) throws ClientPolicyException {
        logger.trace("Backchannel Authentication Endpoint - authn request");

        if (params == null) {
            logger.trace("request parameter not exist.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameters");
        }

        String requestParam = params.getFirst(OIDCLoginProtocol.REQUEST_PARAM);
        String requestUriParam = params.getFirst(OIDCLoginProtocol.REQUEST_URI_PARAM);

        if (requestParam == null && requestUriParam == null) {
            logger.trace("signed authentication request not exist.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter: 'request' or 'request_uri'");
        }

        JsonNode signedAuthReq = (JsonNode)session.getAttribute(BackchannelAuthenticationEndpointRequestParser.CIBA_SIGNED_AUTHENTICATION_REQUEST);

        // check whether signed authentication request exists
        if (signedAuthReq == null || signedAuthReq.isEmpty()) {
            logger.trace("signed authentication request not exist.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter: : 'request' or 'request_uri'");
        }

        // check whether "exp" claim exists
        if (signedAuthReq.get("exp") == null) {
            logger.trace("exp claim not incuded.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter in the signed authentication request: exp");
        }

        // check whether signed authentication request not expired
        long exp = signedAuthReq.get("exp").asLong();
        if (Time.currentTime() > exp) { // TODO: Time.currentTime() is int while exp is long...
            logger.trace("request object expired.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Request Expired");
        }

        // check whether "nbf" claim exists
        if (signedAuthReq.get("nbf") == null) {
            logger.trace("nbf claim not incuded.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter in the signed authentication request: nbf");
        }

        // check whether signed authentication request not yet being processed
        long nbf = signedAuthReq.get("nbf").asLong();
        if (Time.currentTime() < nbf) { // TODO: Time.currentTime() is int while nbf is long...
            logger.trace("request object not yet being processed.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Request not yet being processed");
        }

        // check whether signed authentication request's available period is short
        int availablePeriod = Optional.ofNullable(configuration.getAvailablePeriod()).orElse(DEFAULT_AVAILABLE_PERIOD).intValue();
        if (exp - nbf > availablePeriod) {
            logger.trace("signed authentication request's available period is long.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "signed authentication request's available period is long");
        }

        // check whether "aud" claim exists
        List<String> aud = new ArrayList<String>();
        JsonNode audience = signedAuthReq.get("aud");
        if (audience == null) {
            logger.trace("aud claim not incuded.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter in the 'request' object: aud");
        }
        if (audience.isArray()) {
            for (JsonNode node : audience) aud.add(node.asText());
        } else {
            aud.add(audience.asText());
        }
        if (aud.isEmpty()) {
            logger.trace("aud claim not incuded.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter value in the 'request' object: aud");
        }

        // check whether "aud" claim points to this keycloak as authz server
        String authzServerIss = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), session.getContext().getRealm().getName());
        if (!aud.contains(authzServerIss)) {
            logger.trace("aud not points to the intended realm.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter in the 'request' object: aud");
        }

        // check whether "iss" claim exists
        if (signedAuthReq.get("iss") == null) {
            logger.trace("iss claim not incuded.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter in the 'request' object: iss");
        }

        ClientModel client = session.getContext().getClient();
        String iss = signedAuthReq.get("iss").asText();
        if (!iss.equals(client.getClientId())) {
            logger.trace("iss claim not match client's identity.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Invalid parameter in the 'request' object: iss");
        }

        // check whether "iat" claim exists
        if (signedAuthReq.get("iat") == null) {
            logger.trace("iat claim not incuded.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter in the signed authentication request: iat");
        }

        // check whether "jti" claim exists
        if (signedAuthReq.get("jti") == null) {
            logger.trace("jti claim not incuded.");
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "Missing parameter in the signed authentication request: jti");
        }

        logger.trace("Passed.");
    }

}
