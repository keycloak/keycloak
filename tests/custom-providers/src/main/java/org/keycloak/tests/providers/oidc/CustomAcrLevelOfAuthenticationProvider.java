/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.providers.oidc;

import java.util.Comparator;
import java.util.OptionalInt;

import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLevelOfAuthenticationProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.AcrUtils;

/**
 * Maps custom acr values like {@code level=2,method=otp} to the level given by their {@code level} part. The acr claim of
 * the token is the requested acr value with the highest level reached in the session, including all its parts.
 */
public class CustomAcrLevelOfAuthenticationProvider implements OIDCLevelOfAuthenticationProvider {

    private static final String LEVEL_PREFIX = "level=";

    private final KeycloakSession session;

    public CustomAcrLevelOfAuthenticationProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public OptionalInt getRequestedLoa(ClientModel client, String acrValue) {
        if (!acrValue.startsWith(LEVEL_PREFIX)) {
            return OptionalInt.empty();
        }
        String level = acrValue.substring(LEVEL_PREFIX.length()).split(",")[0];
        return OptionalInt.of(Integer.parseInt(level));
    }

    @Override
    public String getAcr(AuthenticatedClientSessionModel clientSession) {
        ClientModel client = clientSession.getClient();
        int loa = LoAUtil.getCurrentLevelOfAuthentication(clientSession);
        return AcrUtils.getAcrValues(session, clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM),
                        clientSession.getNote(OIDCLoginProtocol.ACR_PARAM), client).stream()
                .filter(acr -> getRequestedLoa(client, acr).orElse(Integer.MAX_VALUE) <= loa)
                .max(Comparator.comparingInt(acr -> getRequestedLoa(client, acr).getAsInt()))
                .orElse(null);
    }

    @Override
    public void close() {

    }
}
