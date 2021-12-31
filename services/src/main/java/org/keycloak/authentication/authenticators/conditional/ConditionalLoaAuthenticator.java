/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowCallback;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

public class ConditionalLoaAuthenticator implements ConditionalAuthenticator, AuthenticationFlowCallback {

    public static final String LEVEL = "loa-condition-level";
    public static final String STORE_IN_USER_SESSION = "loa-store-in-user-session";

    private static final Logger logger = Logger.getLogger(ConditionalLoaAuthenticator.class);

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        int currentLoa = AuthenticatorUtil.getCurrentLevelOfAuthentication(authSession);
        int requestedLoa = AuthenticatorUtil.getRequestedLevelOfAuthentication(authSession);
        Integer configuredLoa = getConfiguredLoa(context);
        return (currentLoa < Constants.MINIMUM_LOA && requestedLoa < Constants.MINIMUM_LOA)
                || ((configuredLoa == null || currentLoa < configuredLoa) && currentLoa < requestedLoa);
    }

    @Override
    public void onParentFlowSuccess(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        Integer newLoa = getConfiguredLoa(context);
        if (newLoa == null) {
            return;
        }
        logger.tracef("Updating LoA to '%d' when authenticating session '%s'", newLoa, authSession.getParentSession().getId());
        authSession.setAuthNote(Constants.LEVEL_OF_AUTHENTICATION, String.valueOf(newLoa));
        if (isStoreInUserSession(context)) {
            authSession.setUserSessionNote(Constants.LEVEL_OF_AUTHENTICATION, String.valueOf(newLoa));
        }
    }

    private Integer getConfiguredLoa(AuthenticationFlowContext context) {
        try {
            return Integer.parseInt(context.getAuthenticatorConfig().getConfig().get(LEVEL));
        } catch (NullPointerException | NumberFormatException e) {
            logger.errorv("Invalid configuration: {0}", LEVEL);
            return null;
        }
    }

    private boolean isStoreInUserSession(AuthenticationFlowContext context) {
        try {
            return Boolean.parseBoolean(context.getAuthenticatorConfig().getConfig().get(STORE_IN_USER_SESSION));
        } catch (NullPointerException | NumberFormatException e) {
            logger.errorv("Invalid configuration: {0}", STORE_IN_USER_SESSION);
            return false;
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) { }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) { }

    @Override
    public void close() { }
}
