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

package org.keycloak.authentication.authenticators.browser;

import java.util.Arrays;
import java.util.Map;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.util.HiddenBrokerContext;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

public class SetHiddenBrokerAuthenticator implements Authenticator {
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.success();

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        if (config == null) return;

        Map<String, String> map = config.getConfig();
        if (map == null) return;

        String mapValue = map.get(SetHiddenBrokerAuthenticatorFactory.HIDDEN_BROKER_CONFIG);
        if (mapValue == null) return;

        String[] values = Constants.CFG_DELIMITER_PATTERN.split(mapValue);
        if (values.length == 0) return;

        HiddenBrokerContext hiddenBrokerContext = new HiddenBrokerContext();
        hiddenBrokerContext.setHiddenBrokers(Arrays.stream(values).toList());
        hiddenBrokerContext.saveToAuthenticationSession(context.getAuthenticationSession());
    }

    @Override
    public void action(AuthenticationFlowContext context) {
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {

    }
}
