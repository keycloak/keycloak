/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authentication.authenticators.conditional;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.jboss.logging.Logger;

/**
 * @author rmartinc
 */
public class ConditionalCredentialAuthenticator implements ConditionalAuthenticator {

    protected static final ConditionalCredentialAuthenticator SINGLETON = new ConditionalCredentialAuthenticator();
    private static final Logger logger = Logger.getLogger(ConditionalCredentialAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        final Map<String, String> config = context.getAuthenticatorConfig() != null
                ? context.getAuthenticatorConfig().getConfig()
                : Collections.emptyMap();
        final Set<String> credentials = new HashSet<>(Arrays.asList(Constants.CFG_DELIMITER_PATTERN.split(
                config.getOrDefault(ConditionalCredentialAuthenticatorFactory.CONF_CREDENTIALS, ""))));
        final boolean included = Boolean.parseBoolean(config.get(ConditionalCredentialAuthenticatorFactory.CONF_INCLUDED));

        List<String> authCredentials = AuthenticatorUtil.getAuthnCredentials(context.getAuthenticationSession());
        if (authCredentials.isEmpty()) {
            authCredentials = Collections.singletonList(ConditionalCredentialAuthenticatorFactory.NONE_CREDENTIAL);
        }
        if (logger.isTraceEnabled()) {
            logger.tracef("Checking if any authentication credential '%s' is %s in %s", authCredentials, included? "included" : "not included", credentials);
        }

        // remove all credentials that are not used in the authentication
        credentials.retainAll(authCredentials);

        return included
                ? !credentials.isEmpty()
                : credentials.isEmpty();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Not used
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Not used
    }

    @Override
    public void close() {
        // Does nothing
    }
}
