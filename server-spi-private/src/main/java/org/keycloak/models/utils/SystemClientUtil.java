/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.utils;

import java.util.Optional;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SystemClientUtil {

    public static final String SYSTEM_CLIENT_ID = "_system";

    private static final Logger logger = Logger.getLogger(SystemClientUtil.class);


    /**
     * @return system client used during usecases when some "metaclient" is needed (EG. For fresh authenticationSession used during actionTokenFlow when email link is opened in new browser)
     */
    public static ClientModel getSystemClient(RealmModel realm) {
        // Try to return builtin "account" client first
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client != null && client.isEnabled()) {
            return client;
        }


        // Fallback to "system" client
        client = realm.getClientByClientId(SYSTEM_CLIENT_ID);
        if (client != null) {
            return client;
        } else {
            // Return system client
            logger.warnf("Client '%s' not available. Creating system client '%s' for system operations", Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, SYSTEM_CLIENT_ID);
            client = realm.addClient(SYSTEM_CLIENT_ID);
            client.setName(SYSTEM_CLIENT_ID);
            return client;
        }

    }

    /**
     * Cleanup system client URL to avoid links to account management
     */
    public static void checkSkipLink(KeycloakSession session, AuthenticationSessionModel authSession) {
        String usedClientId = Optional.ofNullable(authSession)
                .map(it -> it.getClient().getClientId())
                .orElseGet(() -> session.getContext().getUri().getQueryParameters().getFirst(Constants.CLIENT_ID));

        if (usedClientId != null && usedClientId.equals(getSystemClient(session.getContext().getRealm()).getClientId())) {
            session.getProvider(LoginFormsProvider.class).setAttribute(Constants.SKIP_LINK, true);
        }
    }
}
