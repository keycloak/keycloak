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

package org.keycloak.protocol.oidc;

import java.util.Map;
import java.util.OptionalInt;

import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.services.managers.AuthenticationManager;

import org.jboss.logging.Logger;

public class DefaultOIDCLevelOfAuthenticationProvider implements OIDCLevelOfAuthenticationProvider {
    private static final Logger logger = Logger.getLogger(DefaultOIDCLevelOfAuthenticationProvider.class);
    private final KeycloakSession keycloakSession;

    public DefaultOIDCLevelOfAuthenticationProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public OptionalInt getRequestedLoa(ClientModel client, String acrValue) {
        Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(client);

        try {
            Integer loa = acrLoaMap.get(acrValue);
            return OptionalInt.of(loa == null ? Integer.parseInt(acrValue) : loa);
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public String getAcr(AuthenticatedClientSessionModel clientSession) {
        int loa = LoAUtil.getCurrentLevelOfAuthentication(clientSession);
        logger.tracef("Loa level when authenticated to client %s: %d", clientSession.getClient().getClientId(), loa);
        if (loa < Constants.MINIMUM_LOA) {
            loa = AuthenticationManager.isSSOAuthentication(clientSession) ? 0 : 1;
        }

        Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(clientSession.getClient());
        String acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, AcrUtils.getRequiredAcrValues(
                clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM)));
        if (acr == null) {
            acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, AcrUtils.getAcrValues(keycloakSession,
                    clientSession.getNote(OIDCLoginProtocol.CLAIMS_PARAM),
                    clientSession.getNote(OIDCLoginProtocol.ACR_PARAM), clientSession.getClient()));
            if (acr == null) {
                acr = AcrUtils.mapLoaToAcr(loa, acrLoaMap, acrLoaMap.keySet());
                if (acr == null) {
                    acr = String.valueOf(loa);
                }
            }
        }

        logger.tracef("Level sent in the token to client %s: %s. Original loa from the authentication: %d", clientSession.getClient().getClientId(), acr, loa);
        return acr;
    }

    @Override
    public void close() {

    }
}
