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

package org.keycloak.protocol.oidc.grants;

import java.util.Set;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.TokenExchangeContext;
import org.keycloak.protocol.oidc.TokenExchangeProvider;
import org.keycloak.services.CorsErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenExchangeRequestContext;

/**
 * OAuth 2.0 Authorization Code Grant
 * https://datatracker.ietf.org/doc/html/rfc8693#section-2.1
 *
 * @author <a href="mailto:demetrio@carretti.pro">Dmitry Telegin</a> (et al.)
 */
public class TokenExchangeGrantType extends OAuth2GrantTypeBase {

    private static final Set<String> SUPPORTED_DUPLICATED_PARAMETERS = Set.of(OAuth2Constants.AUDIENCE, OAuth2Constants.RESOURCE);

    @Override
    public Response process(Context context) {
        setContext(context);

        event.detail(Details.AUTH_METHOD, "token_exchange");
        event.client(client);

        TokenExchangeContext exchange = new TokenExchangeContext(
                session,
                formParams,
                cors,
                realm,
                event,
                client,
                clientConnection,
                headers,
                tokenManager,
                clientAuthAttributes);

        TokenExchangeProvider tokenExchangeProvider = session.getKeycloakSessionFactory()
                .getProviderFactoriesStream(TokenExchangeProvider.class)
                .sorted((f1, f2) -> f2.order() - f1.order())
                .map(f -> session.getProvider(TokenExchangeProvider.class, f.getId()))
                .filter(p -> p.supports(exchange))
                .findFirst()
                .orElseThrow(() -> {
                    if (exchange.getUnsupportedReason() != null) {
                        event.detail(Details.REASON, exchange.getUnsupportedReason());
                        event.error(Errors.INVALID_REQUEST);
                        return new CorsErrorResponseException(cors, OAuthErrorException.INVALID_REQUEST, exchange.getUnsupportedReason(), Response.Status.BAD_REQUEST);
                    } else {
                        return new InternalServerErrorException("No token exchange provider available");
                    }
                });

        try {
            //trigger if there is a supported token exchange provider
            session.clientPolicy().triggerOnEvent(new TokenExchangeRequestContext(exchange));
        } catch (ClientPolicyException cpe) {
            event.detail(Details.REASON, Details.CLIENT_POLICY_ERROR);
            event.detail(Details.CLIENT_POLICY_ERROR, cpe.getError());
            event.detail(Details.CLIENT_POLICY_ERROR_DETAIL, cpe.getErrorDetail());
            event.error(cpe.getError());
            throw new CorsErrorResponseException(cors, cpe.getError(), cpe.getErrorDetail(), cpe.getErrorStatus());
        }

        return tokenExchangeProvider.exchange(exchange);
    }

    @Override
    public EventType getEventType() {
        return EventType.TOKEN_EXCHANGE;
    }

    @Override
    public Set<String> getSupportedMultivaluedRequestParameters() {
        return SUPPORTED_DUPLICATED_PARAMETERS;
    }
}
