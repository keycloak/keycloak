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

package org.keycloak.testsuite.authentication;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ExpectedParamAuthenticator implements Authenticator {

    public static final String EXPECTED_VALUE = "expected_value";

    public static final String LOGGED_USER = "logged_user";


    private static final Logger logger = Logger.getLogger(ExpectedParamAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String paramValue = context.getAuthenticationSession().getClientNote(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + "foo");
        String expectedValue = context.getAuthenticatorConfig().getConfig().get(EXPECTED_VALUE);
        logger.info("Value: " + paramValue + ", expectedValue: " + expectedValue);

        if (paramValue != null && paramValue.equals(expectedValue)) {

            String loggedUser = context.getAuthenticatorConfig().getConfig().get(LOGGED_USER);
            if (loggedUser == null) {
                logger.info("Successfully authenticated, but don't set any authenticated user");
            } else {
                UserModel user = context.getSession().users().getUserByUsername(context.getRealm(), loggedUser);
                logger.info("Successfully authenticated as user " + user.getUsername());
                context.setUser(user);
            }

            context.success();
        } else {
            context.attempted();
        }
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
