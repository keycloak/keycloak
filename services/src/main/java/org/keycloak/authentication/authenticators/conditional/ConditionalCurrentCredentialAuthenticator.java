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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * @author rmartinc
 */
public class ConditionalCurrentCredentialAuthenticator implements ConditionalAuthenticator {

    protected static final ConditionalCurrentCredentialAuthenticator SINGLETON = new ConditionalCurrentCredentialAuthenticator();
    private static final Logger logger = Logger.getLogger(ConditionalCurrentCredentialAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        final Map<String, String> config = context.getAuthenticatorConfig() != null
                ? context.getAuthenticatorConfig().getConfig()
                : Collections.emptyMap();
        final Set<String> credentials = Set.of(Constants.CFG_DELIMITER_PATTERN.split(
                config.getOrDefault(ConditionalCurrentCredentialAuthenticatorFactory.CONF_CREDENTIALS, "")));
        final boolean included = Boolean.parseBoolean(config.get(ConditionalCurrentCredentialAuthenticatorFactory.CONF_INCLUDED));

        String currentCredential = context.getAuthenticationSession().getAuthNote(AuthenticationProcessor.LAST_AUTHN_CREDENTIAL);
        if (currentCredential == null) {
            currentCredential = ConditionalCurrentCredentialAuthenticatorFactory.NONE_CREDENTIAL;
        }
        if (logger.isTraceEnabled()) {
            logger.tracef("Checking if current credential '%s' is %s in %s", currentCredential, included? "included" : "not included", credentials);
        }

        return included
                ? credentials.contains(currentCredential)
                : !credentials.contains(currentCredential);
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
