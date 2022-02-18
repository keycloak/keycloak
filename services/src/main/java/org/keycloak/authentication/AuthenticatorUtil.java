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

package org.keycloak.authentication;

import com.google.common.collect.Sets;
import org.jboss.logging.Logger;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.keycloak.services.managers.AuthenticationManager.SSO_AUTH;

public class AuthenticatorUtil {

    private static final Logger logger = Logger.getLogger(AuthenticatorUtil.class);

    // It is used for identification of note included in authentication session for storing callback provider factories
    public static String CALLBACKS_FACTORY_IDS_NOTE = "callbacksFactoryProviderIds";


    public static boolean isSSOAuthentication(AuthenticationSessionModel authSession) {
        return "true".equals(authSession.getAuthNote(SSO_AUTH));
    }

    public static boolean isLevelOfAuthenticationForced(AuthenticationSessionModel authSession) {
        return Boolean.parseBoolean(authSession.getClientNote(Constants.FORCE_LEVEL_OF_AUTHENTICATION));
    }

    public static int getRequestedLevelOfAuthentication(AuthenticationSessionModel authSession) {
        String requiredLoa = authSession.getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION);
        return requiredLoa == null ? Constants.NO_LOA : Integer.parseInt(requiredLoa);
    }

    public static int getCurrentLevelOfAuthentication(AuthenticationSessionModel authSession) {
        String authSessionLoaNote = authSession.getAuthNote(Constants.LEVEL_OF_AUTHENTICATION);
        return authSessionLoaNote == null ? Constants.NO_LOA : Integer.parseInt(authSessionLoaNote);
    }

    public static boolean isLevelOfAuthenticationSatisfied(AuthenticationSessionModel authSession) {
        return AuthenticatorUtil.getRequestedLevelOfAuthentication(authSession)
            <= AuthenticatorUtil.getCurrentLevelOfAuthentication(authSession);
    }

    public static int getCurrentLevelOfAuthentication(AuthenticatedClientSessionModel clientSession) {
        String clientSessionLoaNote = clientSession.getNote(Constants.LEVEL_OF_AUTHENTICATION);
        return clientSessionLoaNote == null ? Constants.NO_LOA : Integer.parseInt(clientSessionLoaNote);
    }

    /**
     * Set authentication session note for callbacks defined for {@link AuthenticationFlowCallbackFactory) factories
     *
     * @param authSession   authentication session
     * @param authFactoryId authentication factory ID which should be added to the authentication session note
     */
    public static void setAuthCallbacksFactoryIds(AuthenticationSessionModel authSession, String authFactoryId) {
        if (authSession == null || StringUtil.isBlank(authFactoryId)) return;

        final String callbacksFactories = authSession.getAuthNote(CALLBACKS_FACTORY_IDS_NOTE);

        if (StringUtil.isNotBlank(callbacksFactories)) {
            boolean containsProviderId = callbacksFactories.equals(authFactoryId) ||
                    callbacksFactories.contains(Constants.CFG_DELIMITER + authFactoryId) ||
                    callbacksFactories.contains(authFactoryId + Constants.CFG_DELIMITER);

            if (!containsProviderId) {
                authSession.setAuthNote(CALLBACKS_FACTORY_IDS_NOTE, callbacksFactories + Constants.CFG_DELIMITER + authFactoryId);
            }
        } else {
            authSession.setAuthNote(CALLBACKS_FACTORY_IDS_NOTE, authFactoryId);
        }
    }

    /**
     * Get set of Authentication factories IDs defined in authentication session as CALLBACKS_FACTORY_IDS_NOTE
     *
     * @param authSession authentication session
     * @return set of factories IDs
     */
    public static Set<String> getAuthCallbacksFactoryIds(AuthenticationSessionModel authSession) {
        if (authSession == null) return Collections.emptySet();

        final String callbacksFactories = authSession.getAuthNote(CALLBACKS_FACTORY_IDS_NOTE);

        if (StringUtil.isNotBlank(callbacksFactories)) {
            return Sets.newHashSet(callbacksFactories.split(Constants.CFG_DELIMITER));
        } else {
            return Collections.emptySet();
        }
    }


    /**
     * @param realm
     * @param flowId
     * @param providerId
     * @return all executions of given "provider_id" type. This is deep (recursive) obtain of executions of the particular flow
     */
    public static List<AuthenticationExecutionModel> getExecutionsByType(RealmModel realm, String flowId, String providerId) {
        List<AuthenticationExecutionModel> executions = new LinkedList<>();
        realm.getAuthenticationExecutionsStream(flowId).forEach(authExecution -> {
            if (providerId.equals(authExecution.getAuthenticator())) {
                executions.add(authExecution);
            } else if (authExecution.isAuthenticatorFlow() && authExecution.getFlowId() != null) {
                executions.addAll(getExecutionsByType(realm, authExecution.getFlowId(), providerId));
            }
        });
        return executions;
    }

    /**
     * @param realm
     * @return All LoA numbers configured in the conditions in the realm browser flow
     */
    public static Stream<Integer> getLoAConfiguredInRealmBrowserFlow(RealmModel realm) {
        List<AuthenticationExecutionModel> loaConditions = getExecutionsByType(realm, realm.getBrowserFlow().getId(), ConditionalLoaAuthenticatorFactory.PROVIDER_ID);
        if (loaConditions.isEmpty()) {
            // Default values used when step-up conditions not used in the browser authentication flow.
            // This is used for backwards compatibility and in case when step-up is not configured in the authentication flow (returning 1 in case of "normal" authentication, 0 for SSO authentication)
            return Stream.of(Constants.MINIMUM_LOA, 1);
        } else {
            Stream<Integer> configuredLoas = loaConditions.stream()
                    .map(authExecution -> realm.getAuthenticatorConfigById(authExecution.getAuthenticatorConfig()))
                    .filter(Objects::nonNull)
                    .map(authConfig -> {
                        String levelAsStr = authConfig.getConfig().get(ConditionalLoaAuthenticator.LEVEL);
                        try {
                            // Check it can be cast to number
                            return Integer.parseInt(levelAsStr);
                        } catch (NullPointerException | NumberFormatException e) {
                            logger.warnf("Invalid level '%s' configured for the configuration of LoA condition with alias '%s'. Level should be number.", levelAsStr, authConfig.getAlias());
                            return null;
                        }
                    })
                    .filter(Objects::nonNull);

            // Add 0 as a level used for SSO cookie
            return Stream.concat(Stream.of(Constants.MINIMUM_LOA), configuredLoas);
        }
    }
}
