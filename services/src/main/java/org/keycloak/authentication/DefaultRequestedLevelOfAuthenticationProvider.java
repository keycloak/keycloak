/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;
import java.util.Optional;

public class DefaultRequestedLevelOfAuthenticationProvider implements RequestedLevelOfAuthenticationProvider {
    private final KeycloakSession keycloakSession;
    private static final Logger logger = Logger.getLogger(DefaultRequestedLevelOfAuthenticationProvider.class);

    public DefaultRequestedLevelOfAuthenticationProvider(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public Optional<Integer> getRequestedLoa(String acrValue) {
        AuthenticationSessionModel authSession = this.keycloakSession.getContext().getAuthenticationSession();
        Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(authSession.getClient());

        try {
            Integer loa = acrLoaMap.get(acrValue);
            return loa == null ? Optional.of(Integer.parseInt(acrValue)) : Optional.of(loa);
        } catch (NumberFormatException e) {
            // this is an unknown acr. In case of an essential claim, we directly reject authentication as we cannot met the specification requirement. Otherwise fallback to minimum LoA
            boolean essential = Boolean.parseBoolean(authSession.getClientNote(Constants.FORCE_LEVEL_OF_AUTHENTICATION));
            if (essential) {
                logger.errorf("Requested essential acr value '%s' is not a number and it is not mapped in the ACR-To-Loa mappings of realm or client. Please doublecheck ACR-to-LOA mapping or correct ACR passed in the 'claims' parameter.", acrValue);
                throw new ErrorPageException(keycloakSession, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_PARAMETER, OIDCLoginProtocol.CLAIMS_PARAM);
            } else {
                logger.warnf("Requested acr value '%s' is not a number and it is not mapped in the ACR-To-Loa mappings of realm or client. Please doublecheck ACR-to-LOA mapping or correct used ACR.", acrValue);
                return Optional.of(Constants.MINIMUM_LOA);
            }
        }
    }

    @Override
    public void close() {

    }
}
