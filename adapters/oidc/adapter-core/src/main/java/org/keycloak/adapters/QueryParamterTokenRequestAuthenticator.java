/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters;

import org.jboss.logging.Logger;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * @author <a href="mailto:froehlich.ch@gmail.com">Christian Froehlich</a>
 * @author <a href="mailto:brad.culley@spartasystems.com">Brad Culley</a>
 * @author <a href="mailto:john.ament@spartasystems.com">John D. Ament</a>
 * @version $Revision: 1 $
 */
public class QueryParamterTokenRequestAuthenticator extends BearerTokenRequestAuthenticator {
    public static final String ACCESS_TOKEN = "access_token";
    protected Logger log = Logger.getLogger(QueryParamterTokenRequestAuthenticator.class);

    public QueryParamterTokenRequestAuthenticator(KeycloakDeployment deployment) {
        super(deployment);
    }

    public AuthOutcome authenticate(HttpFacade exchange) {
        if(!deployment.isOAuthQueryParameterEnabled()) {
            return AuthOutcome.NOT_ATTEMPTED;
        }
        tokenString = null;
        tokenString = getAccessTokenFromQueryParamter(exchange);
        if (tokenString == null || tokenString.trim().isEmpty()) {
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.NO_QUERY_PARAMETER_ACCESS_TOKEN, null, null);
            return AuthOutcome.NOT_ATTEMPTED;
        }
        return (authenticateToken(exchange, tokenString));
    }

    String getAccessTokenFromQueryParamter(HttpFacade exchange) {
        try {
            if (exchange != null && exchange.getRequest() != null) {
                return exchange.getRequest().getQueryParamValue(ACCESS_TOKEN);
            }
        } catch (Exception ignore) {
        }
        return null;
    }
}
