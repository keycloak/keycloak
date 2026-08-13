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

package org.keycloak.ipatuura_user_spi.authenticator;

import jakarta.ws.rs.core.HttpHeaders;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.KeycloakSession;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jstephen@redhat.com.com">Justin Stephenson</a>
 * @version $Revision: 1 $
 */
public class IpatuuraAuthenticator {

    private static final Logger logger = Logger.getLogger(IpatuuraAuthenticator.class);

    public String getToken(KeycloakSession session) {
        HttpHeaders headers = session.getContext().getHttpRequest().getHttpHeaders();

        String authHeader = headers.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null) {
            logger.debug("authHeader == NULL()!");
            return null;
        }
        String[] tokens = authHeader.split(" ");

        if (tokens.length == 0) { // assume not supported
            logger.debug("Invalid length of tokens: " + tokens.length);
            return null;
        }
        if (!KerberosConstants.NEGOTIATE.equalsIgnoreCase(tokens[0])) {
            logger.debug("Unknown scheme " + tokens[0]);
            return null;
        }
        if (tokens.length != 2) {
            logger.debug("Invalid credentials tokens.length != 2");
            return null;
        }

        String spnegoToken = tokens[1];

        return spnegoToken;
    }
}
