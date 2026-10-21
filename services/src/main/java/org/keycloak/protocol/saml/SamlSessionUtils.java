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

package org.keycloak.protocol.saml;

import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SamlSessionUtils {

    private static final String DELIMITER = "::";

    // Just perf optimization
    private static final Pattern PATTERN = Pattern.compile(DELIMITER);


    public static String getSessionIndex(AuthenticatedClientSessionModel clientSession) {
        UserSessionModel userSession = clientSession.getUserSession();
        ClientModel client = clientSession.getClient();

        return userSession.getId() + DELIMITER + client.getId();
    }


    public static AuthenticatedClientSessionModel getClientSession(KeycloakSession session, RealmModel realm, String sessionIndex) {
        if (sessionIndex == null) {
            return null;
        }

        String[] parts = PATTERN.split(sessionIndex);
        if (parts.length != 2) {
            return null;
        }

        String userSessionId = parts[0];
        String clientUUID = parts[1];
        UserSessionModel userSession = session.sessions().getUserSessionIfClientExists(realm, userSessionId, false, clientUUID);
        if (userSession == null) {
            return null;
        }

        return userSession.getAuthenticatedClientSessionByClient(clientUUID);
    }

    public static Iterator<SamlAuthenticationPreprocessor> getSamlAuthenticationPreprocessorIterator(KeycloakSession session) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(SamlAuthenticationPreprocessor.class)
                .filter(Objects::nonNull)
                .map(SamlAuthenticationPreprocessor.class::cast)
                .iterator();
    }

}
