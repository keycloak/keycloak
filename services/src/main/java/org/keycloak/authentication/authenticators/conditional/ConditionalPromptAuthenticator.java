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


import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;

/** 
 * Conditional authenticator to check the 'prompt' parameter in authentication requests.
 * Prompt is a standard parameter defined in OpenId Connect specification (https://openid.net/specs/openid-connect-core-1_0.html#AuthRequest).
 * 
 * Useful for example when you need to avoid SPNEGO (Kerberos) negotiation (with 'prompt=login').
 */
public class ConditionalPromptAuthenticator implements ConditionalAuthenticator {
    
    protected static final ConditionalPromptAuthenticator SINGLETON = new ConditionalPromptAuthenticator();

    private static Logger logger = Logger.getLogger(ConditionalPromptAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        final AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null || configModel.getConfig() == null) {
            logger.warnf("No configuration defined for the conditional prompt param authenticator.");
            return false;
        }

        final String promptValue = configModel.getConfig().get(ConditionalPromptAuthenticatorFactory.PROMPT_PROPERTY_NAME);
        boolean negateOutput = Boolean.parseBoolean(configModel.getConfig().get(ConditionalPromptAuthenticatorFactory.CONF_NEGATE));
        if (promptValue == null) {
            logger.warnf("No prompt param value configured in the option '%s' of the configuration '%s'.", ConditionalPromptAuthenticatorFactory.PROMPT_PROPERTY_NAME, configModel.getAlias());
            return false;
        }

        boolean promptMaching = true;
        AuthenticationSessionModel session = context.getAuthenticationSession();
        
        Map<String, String> clientNotes = session.getClientNotes();
        String prompt = clientNotes.get(OIDCLoginProtocol.PROMPT_PARAM);
        if (prompt == null) {
            promptMaching = promptValue.equals(ConditionalPromptAuthenticatorFactory.PROMPT_VALUE_NOT_DEFINED);
        } else {
            promptMaching = promptValue.equals(prompt);
        }
        return negateOutput != promptMaching;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
         // no-op
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // no-op
    }

    @Override
    public void close() {
        // no-op
    }
}
