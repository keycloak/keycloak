/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;
import java.util.Objects;

public class ConditionalUserAttributeValue implements ConditionalAuthenticator {

    static final ConditionalUserAttributeValue SINGLETON = new ConditionalUserAttributeValue();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        // Retrieve configuration
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        String attributeName = config.get(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_NAME);
        String attributeValue = config.get(ConditionalUserAttributeValueFactory.CONF_ATTRIBUTE_EXPECTED_VALUE);
        boolean negateOutput = Boolean.parseBoolean(config.get(ConditionalUserAttributeValueFactory.CONF_NOT));

        UserModel user = context.getUser();
        if (user == null) {
            throw new AuthenticationFlowException("Cannot find user for obtaining particular user attributes. Authenticator: " + ConditionalUserAttributeValueFactory.PROVIDER_ID, AuthenticationFlowError.UNKNOWN_USER);
        }

        boolean result = user.getAttributeStream(attributeName).anyMatch(attr -> Objects.equals(attr, attributeValue));
        return negateOutput != result;
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
