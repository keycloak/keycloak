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
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.AuthenticatorUtil;
import org.keycloak.authentication.authenticators.util.AcrStore;
import org.keycloak.authentication.authenticators.util.LoAUtil;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

public class ConditionalLoaAuthenticator implements ConditionalAuthenticator, AuthenticationFlowCallback {
    public static final String LEVEL = "loa-condition-level";
    public static final String MAX_AGE = "loa-max-age";
    public static final int DEFAULT_MAX_AGE = 36000; // 10 days

    // Only for backwards compatibility with Keycloak 17
    @Deprecated
    public static final String STORE_IN_USER_SESSION = "loa-store-in-user-session";

    private static final Logger logger = Logger.getLogger(ConditionalLoaAuthenticator.class);

    private final KeycloakSession session;

    public ConditionalLoaAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        AcrStore acrStore = new AcrStore(authSession);
        int currentAuthenticationLoa = acrStore.getLevelOfAuthenticationFromCurrentAuthentication();
        Integer configuredLoa = getConfiguredLoa(context);
        if (configuredLoa == null) configuredLoa = Constants.MINIMUM_LOA;
        int requestedLoa = acrStore.getRequestedLevelOfAuthentication();
        if (currentAuthenticationLoa < Constants.MINIMUM_LOA) {
            logger.tracef("Condition '%s' evaluated to true due the user not yet reached any authentication level in this session, configuredLoa: %d, requestedLoa: %d",
                    context.getAuthenticatorConfig().getAlias(), configuredLoa, requestedLoa);
            return true;
        } else {
            if (requestedLoa < configuredLoa) {
                logger.tracef("Condition '%s' evaluated to false due the requestedLoa '%d' smaller than configuredLoa '%d'. CurrentAuthenticationLoa: %d",
                        context.getAuthenticatorConfig().getAlias(), requestedLoa, configuredLoa, currentAuthenticationLoa);
                return false;
            }
            int maxAge = getMaxAge(context);
            boolean previouslyAuthenticated = (acrStore.isLevelAuthenticatedInPreviousAuth(configuredLoa, maxAge));
            if (previouslyAuthenticated) {
                if (currentAuthenticationLoa < configuredLoa) {
                    acrStore.setLevelAuthenticatedToCurrentRequest(configuredLoa);
                }
            }
            logger.tracef("Checking condition '%s' : currentAuthenticationLoa: %d, requestedLoa: %d, configuredLoa: %d, evaluation result: %b",
                    context.getAuthenticatorConfig().getAlias(), currentAuthenticationLoa, requestedLoa, configuredLoa, !previouslyAuthenticated);

            return !previouslyAuthenticated;
        }
    }

    @Override
    public void onParentFlowSuccess(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        AcrStore acrStore = new AcrStore(authSession);

        Integer newLoa = getConfiguredLoa(context);
        if (newLoa == null) {
            return;
        }
        int maxAge = getMaxAge(context);
        if (maxAge == 0) {
            logger.tracef("Skip updating authenticated level '%d' in condition '%s' for future authentications due max-age set to 0", newLoa, context.getAuthenticatorConfig().getAlias());
            acrStore.setLevelAuthenticatedToCurrentRequest(newLoa);
        } else {
            logger.tracef("Updating LoA to '%d' in the condition '%s' when authenticating session '%s'. Max age is %d.",
                    newLoa, context.getAuthenticatorConfig().getAlias(), authSession.getParentSession().getId(), maxAge);
            acrStore.setLevelAuthenticated(newLoa);
        }
    }

    @Override
    public void onTopFlowSuccess() {
        AuthenticationSessionModel authSession = session.getContext().getAuthenticationSession();
        AcrStore acrStore = new AcrStore(authSession);

        logger.tracef("Finished authentication at level %d when authenticating authSession '%s'.", acrStore.getLevelOfAuthenticationFromCurrentAuthentication(), authSession.getParentSession().getId());
        if (acrStore.isLevelOfAuthenticationForced() && !acrStore.isLevelOfAuthenticationSatisfiedFromCurrentAuthentication()) {
            String details = String.format("Forced level of authentication did not meet the requirements. Requested level: %d, Fulfilled level: %d",
                    acrStore.getRequestedLevelOfAuthentication(), acrStore.getLevelOfAuthenticationFromCurrentAuthentication());
            throw new AuthenticationFlowException(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, details, Messages.ACR_NOT_FULFILLED);
        }

        logger.tracef("Updating authenticated levels in authSession '%s' to user session note for future authentications: %s", authSession.getParentSession().getId(), authSession.getAuthNote(Constants.LOA_MAP));
        authSession.setUserSessionNote(Constants.LOA_MAP, authSession.getAuthNote(Constants.LOA_MAP));
    }

    private Integer getConfiguredLoa(AuthenticationFlowContext context) {
       return LoAUtil.getLevelFromLoaConditionConfiguration(context.getAuthenticatorConfig());
    }

    private int getMaxAge(AuthenticationFlowContext context) {
        return LoAUtil.getMaxAgeFromLoaConditionConfiguration(context.getAuthenticatorConfig());
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
