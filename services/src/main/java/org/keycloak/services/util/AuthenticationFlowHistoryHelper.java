/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.services.util;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Used to track executions visited by user during authentication. Useful for form "back" button
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthenticationFlowHistoryHelper {

    private static final Logger log = Logger.getLogger(AuthenticationFlowHistoryHelper.class);

    /**
     * Prefix of the authentication session note with the list of IDs of successful authentication action executions.
     * Those corresponds with the authenticator executions, which were shown to the user.
     *
     * The suffix of the note is the actual path of the authentication flow, so we can track history of more flow types during same authentication session
     *
     * IDs are divided by {@link #DELIMITER}
     */
    private static final String SUCCESSFUL_ACTION_EXECUTIONS = "successful.action.executions";


    private static final String DELIMITER = "::";

    // Just perf optimization
    private static final Pattern PATTERN = Pattern.compile(DELIMITER);

    private final AuthenticationSessionModel authenticationSession;
    private final String flowPath;
    private final String authNoteName;

    public AuthenticationFlowHistoryHelper(AuthenticationProcessor processor) {
        this(processor.getAuthenticationSession(), processor.getFlowPath());
    }

    public AuthenticationFlowHistoryHelper(AuthenticationSessionModel authenticationSession, String flowPath) {
        this.authenticationSession = authenticationSession;
        this.flowPath = flowPath;
        this.authNoteName = SUCCESSFUL_ACTION_EXECUTIONS + DELIMITER + flowPath;
    }


    /**
     * Push executionId to the history if it's not already there
     *
     * @param executionId
     */
    public void pushExecution(String executionId) {
        if (containsExecution(executionId)) {
            log.tracef("Not adding execution %s to authentication session. Execution is already there", executionId);
            return;
        }

        log.tracef("Adding execution %s to authentication session. Flow path: %s", executionId, flowPath);

        String history = authenticationSession.getAuthNote(authNoteName);

        history = (history == null) ? executionId : history + DELIMITER + executionId;
        authenticationSession.setAuthNote(authNoteName, history);
    }


    /**
     * Check if there is any executionId in the history
     *
     * @return
     */
    public boolean hasAnyExecution() {
        return authenticationSession.getAuthNote(authNoteName) != null;
    }


    /**
     * Return the last executionId from the history and remove it from the history.
     *
     * @return
     */
    public String pullExecution() {
        String history = authenticationSession.getAuthNote(authNoteName);

        if (history == null) {
            return null;
        }

        String[] splits = PATTERN.split(history);

        String lastActionExecutionId = splits[splits.length - 1];

        if (splits.length == 1) {
            authenticationSession.removeAuthNote(authNoteName);
        } else {
            String newHistory = history.substring(0, history.length() - DELIMITER.length() - lastActionExecutionId.length());
            authenticationSession.setAuthNote(authNoteName, newHistory);
        }

        log.tracef("Returning to execution %s in the authentication session. Flow path: %s", lastActionExecutionId, flowPath);

        return lastActionExecutionId;
    }


    private boolean containsExecution(String executionId) {
        String history = authenticationSession.getAuthNote(authNoteName);

        if (history == null) {
            return false;
        }

        String[] splits = PATTERN.split(history);
        return Arrays.asList(splits).contains(executionId);
    }

}
