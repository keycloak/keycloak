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

package org.keycloak.protocol.oidc.utils;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;

/**
 * @author Ben Cresitello-Dittmar
 * Utility for parsing authenticator method reference (AMR) values.
 */
public class AmrUtils {
    private static final Logger logger = Logger.getLogger(AmrUtils.class);

    /**
     * Resolve the authentication executions whose reference values the amr claim reports for the client session an
     * authentication is attaching to, in the same way the level of authentication is fixed on the client session at that
     * point. An execution completed during the attaching authentication is always reported. An execution carried over from
     * an earlier authentication in the SSO session is reported only while within its configured max age, evaluated now.
     * Every token of the client session, refreshed ones included, then reports the same methods, as it reports the same
     * auth_time.
     *
     * @param authSession The authentication session being attached
     * @param userSession The user session the authentication attached to. Its completed executions already include the attaching authentication's own.
     * @param realm The realm the executions are configured in
     * @return The serialized list of execution ids, an empty list when the client session reports no method
     */
    public static String resolveAuthenticationExecutions(AuthenticationSessionModel authSession, UserSessionModel userSession, RealmModel realm) {
        Set<String> current = AuthenticatorUtils.parseExecutionIds(authSession.getAuthNote(Constants.AUTHENTICATORS_COMPLETED_CURRENT_AUTHENTICATION));
        Map<String, Integer> completed = AuthenticatorUtils.parseCompletedExecutions(userSession.getNote(Constants.AUTHENTICATORS_COMPLETED));

        Set<String> resolved = new LinkedHashSet<>(current);
        completed.forEach((executionId, completedAt) -> {
            if (resolved.contains(executionId)) {
                return;
            }
            Map<String, String> config = getExecutionConfig(executionId, realm);
            if (config != null && carriesOver(config, completedAt)) {
                resolved.add(executionId);
            }
        });
        logger.debugf("amr executions %s resolved for the client session: completed in this authentication %s, completed in the user session %s", resolved, current, completed);

        try {
            return JsonSerialization.writeValueAsString(resolved);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the configured authenticator reference values for executions resolved by
     * {@link #resolveAuthenticationExecutions(AuthenticationSessionModel, UserSessionModel, RealmModel)}. The max age was
     * applied when they were resolved, so it is not applied again. An execution without a configured value is skipped.
     *
     * @param executionIds The authenticator execution ids
     * @param realmModel The realm the executions are configured in
     * @return The list of amr values.
     */
    public static List<String> getAuthenticationExecutionReferences(Collection<String> executionIds, RealmModel realmModel) {
        return executionIds.stream()
            .map(executionId -> {
                Map<String, String> config = getExecutionConfig(executionId, realmModel);
                return config == null ? null : config.get(Constants.AUTHENTICATION_EXECUTION_REFERENCE_VALUE);
            })
            .filter(ref -> ref != null && !ref.isEmpty())
            .collect(Collectors.toList());
    }

    /**
     * Decide whether an execution completed by an earlier authentication in the SSO session is still reported by an
     * authentication completing now. A max age of 0, the default, never carries over: the reference value belongs to
     * the authentication that completed the execution only. A positive max age carries over while the execution
     * completed within that many seconds, inclusive.
     *
     * @param config The authenticator execution config
     * @param completedAt The time the execution completed
     * @return True if the authentication completing now reports the execution
     */
    public static boolean carriesOver(Map<String, String> config, Integer completedAt) {
        try {
            int maxAge = Integer.parseInt(config.getOrDefault(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE, "0"));
            return maxAge > 0 && completedAt + maxAge >= Time.currentTime();
        } catch (NumberFormatException e) {
            logger.warnf("invalid authentication execution max age '%s'", config.get(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE));
            return false;
        }
    }

    private static Map<String, String> getExecutionConfig(String executionId, RealmModel realm) {
        AuthenticationExecutionModel execution = realm.getAuthenticationExecutionById(executionId);
        if (execution == null || execution.getAuthenticatorConfig() == null) {
            return null;
        }
        AuthenticatorConfigModel config = realm.getAuthenticatorConfigById(execution.getAuthenticatorConfig());
        return config == null ? null : config.getConfig();
    }

    /**
     * Get the configured authenticator reference values for the specified completed executions, each subject to its
     * configured max age as of now. Used for client sessions without resolved executions. If no
     * value is configured for the execution, null is returned instead of throwing an error.
     *
     * @param executions Map of authenticator execution ids to the time the execution completed
     * @param realmModel The realm the executions are configured in
     * @return The list of amr values.
     */
    public static List<String> getAuthenticationExecutionReferences(Map<String, Integer> executions, RealmModel realmModel) {
        return executions.entrySet().stream()
            .map(
                entry -> {
                    try {
                        // extract the authenticator config and get the authenticator reference value
                        Map<String, String> config = realmModel.getAuthenticatorConfigById(realmModel.getAuthenticationExecutionById(entry.getKey()).getAuthenticatorConfig()).getConfig();
                        if (isAmrValid(config, entry.getValue())){
                            return config.get(Constants.AUTHENTICATION_EXECUTION_REFERENCE_VALUE);
                        }
                    } catch (NullPointerException e){
                        return null;
                    }

                    return null;
                }
            ).filter(
                ref -> ref != null && !ref.isEmpty()
            ).collect(Collectors.toList());
    }

    /**
     * Check if a carried over AMR is still valid by determining if the execution time + the configured max age is not yet in the past
     * @param config The authenticator execution config
     * @param authTime The time that the authentication occurred
     * @return True if the amr value is still valid for this session
     */
    public static boolean isAmrValid(Map<String, String> config, Integer authTime){
        try {
            int maxAge = Integer.parseInt(config.getOrDefault(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE, "0"));
            return authTime + maxAge >= Time.currentTime();
        } catch (NumberFormatException e){
            logger.warnf("invalid authentication execution max age '%s'", config.get(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE));
        }
        return false;
    }
}
