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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;

import org.jboss.logging.Logger;

/**
 * @author Ben Cresitello-Dittmar
 * Utility for parsing authenticator method reference (AMR) values.
 */
public class AmrUtils {
    private static final Logger logger = Logger.getLogger(AmrUtils.class);

    /**
     * Get the configured authenticator reference values for the specified executions, each subject to its configured
     * max age. If no value is configured for the execution, null is returned instead of throwing an error.
     *
     * @param executions Map of authenticator execution ids to the time the execution completed
     * @param realmModel The realm the executions are configured in
     * @return The list of amr values.
     */
    public static List<String> getAuthenticationExecutionReferences(Map<String, Integer> executions, RealmModel realmModel) {
        return getAuthenticationExecutionReferences(executions, Collections.emptySet(), realmModel);
    }

    /**
     * Get the configured authenticator reference values for the specified executions. An execution completed by the
     * authentication that attached to the client session is always reported, whether or not the user session's completed
     * executions still list it. Any other execution was carried over from an earlier authentication in the SSO session and
     * is reported only while within its configured max age. If no value is configured for the execution, null is returned
     * instead of throwing an error.
     *
     * @param executions Map of authenticator execution ids to the time the execution completed
     * @param currentAuthenticationExecutions The execution ids completed by the authentication that attached to the client session
     * @param realmModel The realm the executions are configured in
     * @return The list of amr values.
     */
    public static List<String> getAuthenticationExecutionReferences(Map<String, Integer> executions, Set<String> currentAuthenticationExecutions, RealmModel realmModel) {
        Stream<String> current = currentAuthenticationExecutions.stream()
            .map(executionId -> getAuthenticationExecutionReference(executionId, realmModel));
        Stream<String> carriedOver = executions.entrySet().stream()
            .filter(entry -> !currentAuthenticationExecutions.contains(entry.getKey()))
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
            );
        return Stream.concat(current, carriedOver)
            .filter(ref -> ref != null && !ref.isEmpty())
            .collect(Collectors.toList());
    }

    private static String getAuthenticationExecutionReference(String executionId, RealmModel realmModel) {
        try {
            return realmModel.getAuthenticatorConfigById(realmModel.getAuthenticationExecutionById(executionId).getAuthenticatorConfig()).getConfig()
                .get(Constants.AUTHENTICATION_EXECUTION_REFERENCE_VALUE);
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * Check if an AMR carried over from an earlier authentication in the SSO session is still valid by determining if the
     * execution time + the configured max age is not yet in the past. The default max age of 0 means the reference value
     * belongs to the authentication that completed the execution only, so it never carries over.
     * @param config The authenticator execution config
     * @param authTime The time that the authentication occurred
     * @return True if the amr value is still valid for this session
     */
    public static boolean isAmrValid(Map<String, String> config, Integer authTime){
        try {
            int maxAge = Integer.parseInt(config.getOrDefault(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE, "0"));
            return maxAge > 0 && authTime + maxAge >= Time.currentTime();
        } catch (NumberFormatException e){
            logger.warnf("invalid authentication execution max age '%s'", config.get(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE));
        }
        return false;
    }
}
