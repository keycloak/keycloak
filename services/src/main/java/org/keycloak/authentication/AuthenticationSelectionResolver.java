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

package org.keycloak.authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

/**
 * Resolves set of AuthenticationSelectionOptions
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class AuthenticationSelectionResolver {

    private static final Logger logger = Logger.getLogger(AuthenticationSelectionResolver.class);

    /**
     * This method creates the list of authenticators that is presented to the user. For a required execution, this is
     * only the credentials associated to the authenticator, and for an alternative execution, this is all other alternative
     * executions in the flow, including the credentials.
     * <p>
     * In both cases, the credentials take precedence, with the order selected by the user (or his administrator).
     * <p>
     * The implementation needs to take various subflows into account.
     *
     * For example during configuration of the authentication flow like this:
     * - WebAuthn:                 ALTERNATIVE
     * - Password-and-OTP subflow:  ALTERNATIVE
     *   - Password REQUIRED
     *   - OTP      REQUIRED
     * The user can authenticate with: WebAuthn OR (Password AND OTP). In this case, the user should be able to choose between WebAuthn and Password
     * even if those mechanisms are in different subflows
     *
     * @param model The current execution model
     * @return an ordered list of the authentication selection options to present the user.
     */
    static List<AuthenticationSelectionOption> createAuthenticationSelectionList(AuthenticationProcessor processor, AuthenticationExecutionModel model) {
        List<AuthenticationSelectionOption> authenticationSelectionList = new ArrayList<>();
        List<AuthenticationSelectionOption> userlessCredBasedAuthenticationSelectionList = new ArrayList<>();

        AuthenticationSessionModel authSession = processor.getAuthenticationSession();
        if (authSession != null) {

            List<AuthenticationExecutionModel> executionCandidates = new ArrayList<>();

            String topFlowId = getFlowIdOfTheHighestUsefulFlow(processor, model);

            if (topFlowId == null) {
                addSimpleAuthenticationExecution(processor, model, executionCandidates);
            } else {
                addAllExecutionsFromSubflow(processor, topFlowId, executionCandidates);
            }

            KeycloakSession session = processor.getSession();
            UserModel authenticatedUser = authSession.getAuthenticatedUser();
            if (authenticatedUser != null) {
                // Add credential authenticators in order
                SubjectCredentialManager scm = authenticatedUser.credentialManager();
                Stream<String> storedCredentialTypes = scm.getStoredCredentialsStream().map(CredentialModel::getType);
                Stream<String> configuredCredentialTypes = scm.getConfiguredUserStorageCredentialTypesStream();
                Set<String> allowedCredentialTypes = Stream.concat(storedCredentialTypes, configuredCredentialTypes).collect(Collectors.toSet());

                executionCandidates.removeIf(execution -> {
                    Authenticator authenticator = session.getProvider(Authenticator.class, execution.getAuthenticator());
                    if (authenticator instanceof CredentialValidator) {
                        CredentialValidator<?> cv = (CredentialValidator<?>)authenticator;
                        return !allowedCredentialTypes.contains(cv.getType(session));
                    }
                    return false;
                });
            } else {
                // No user associated with session. Check if this flow contains executions linked to authenticators that don't require a user
                executionCandidates.removeIf(execution -> {
                    Authenticator authenticator = session.getProvider(Authenticator.class, execution.getAuthenticator());
                    return authenticator.requiresUser();
                });
            }

            authenticationSelectionList = executionCandidates.stream()
                    .map(exec -> new AuthenticationSelectionOption(session, exec))
                    .collect(Collectors.toList());
        }

        logger.debugf("Selections when trying execution '%s' : %s", model.getAuthenticator(), authenticationSelectionList);

        return authenticationSelectionList;
    }


    /**
     * Return the flowId of the "highest" subflow, which we need to take into account when creating list of authentication mechanisms
     * shown to the user.
     *
     * For example during configuration of the authentication flow like this:
     * - WebAuthn:                 ALTERNATIVE
     * - Password-and-OTP subflow:  ALTERNATIVE
     *   - Password REQUIRED
     *   - OTP      REQUIRED
     *
     * and assuming that "execution" parameter is PasswordForm, we also need to take the higher subflow into account as user
     * should be able to choose among WebAuthn and Password
     *
     * @param processor
     * @param execution
     * @return
     */
    private static String getFlowIdOfTheHighestUsefulFlow(AuthenticationProcessor processor, AuthenticationExecutionModel execution) {
        String flowId = null;
        RealmModel realm = processor.getRealm();

        while (true) {
            if (execution.isAlternative()) {
                //Consider parent flow as we need to get all alternative executions to be able to list their credentials
                flowId = execution.getParentFlow();
            } else if (execution.isRequired()  || execution.isConditional()) {
                if (execution.isAuthenticatorFlow()) {
                    flowId = execution.getFlowId();
                }

                // Find the corresponding execution. If it is 1st REQUIRED execution in the particular subflow, we need to consider parent flow as well
                List<AuthenticationExecutionModel> executions = realm.getAuthenticationExecutionsStream(execution.getParentFlow())
                        .collect(Collectors.toList());
                int executionIndex = executions.indexOf(execution);
                if (executionIndex != 0) {
                    return flowId;
                } else {
                    flowId = execution.getParentFlow();
                }
            }

            AuthenticationFlowModel flow = realm.getAuthenticationFlowById(flowId);
            if (flow.isTopLevel()) {
                return flowId;
            }
            execution = realm.getAuthenticationExecutionByFlowId(flowId);
        }
    }


    // Process single authenticaion execution, which does NOT point to authentication flow.
    // Fill the typeAuthExecMap and nonCredentialExecutions accordingly
    private static void addSimpleAuthenticationExecution(AuthenticationProcessor processor, AuthenticationExecutionModel execution, List<AuthenticationExecutionModel> executionCandidates) {
        // Don't add already processed executions
        if (DefaultAuthenticationFlow.isProcessed(processor, execution)) {
            return;
        }

        // Skip non executable sub-flows as execution candidates
        if (execution.isAuthenticatorFlow()) {
            return;
        }

        executionCandidates.add(execution);
    }


    /**
     * Fill the typeAuthExecMap and nonCredentialExecutions collections with all available authentication mechanisms for the particular subflow with
     * given flowId
     *
     * Return true if at least something was added to any of the list
     */
    private static boolean addAllExecutionsFromSubflow(AuthenticationProcessor processor, String flowId, List<AuthenticationExecutionModel> executionCandidates) {
        AuthenticationFlowModel flowModel = processor.getRealm().getAuthenticationFlowById(flowId);
        if (flowModel == null) {
            throw new AuthenticationFlowException("Flow not found", AuthenticationFlowError.INTERNAL_ERROR);
        }

        DefaultAuthenticationFlow flow = new DefaultAuthenticationFlow(processor, flowModel);

        logger.debugf("Going through the flow '%s' for adding executions", flowModel.getAlias());

        List<AuthenticationExecutionModel> requiredList = new ArrayList<>();
        List<AuthenticationExecutionModel> alternativeList = new ArrayList<>();
        flow.fillListsOfExecutions(processor.getRealm().getAuthenticationExecutionsStream(flowId), requiredList, alternativeList);

        // If requiredList is not empty, we're going to collect just very first execution from the flow
        if (!requiredList.isEmpty()) {
            AuthenticationExecutionModel requiredExecution = requiredList.stream().filter(ex -> {

                if (ex.isRequired()) return true;

                // For conditional execution, we must check if condition is true. Otherwise return false, which means trying next
                // requiredExecution in the list
                return !flow.isConditionalSubflowDisabled(ex);

            }).findFirst().orElse(null);

            // Not requiredExecution found. Returning false as we did not add any authenticator
            if (requiredExecution == null) return false;

            // Don't add already processed executions
            if (flow.isProcessed(requiredExecution)) {
                return false;
            }

            FormAuthenticatorFactory factory = (FormAuthenticatorFactory) processor.getSession().getKeycloakSessionFactory().getProviderFactory(FormAuthenticator.class, requiredExecution.getAuthenticator());

            // Recursively add credentials from required execution
            if (requiredExecution.isAuthenticatorFlow() && factory == null) {
                return addAllExecutionsFromSubflow(processor, requiredExecution.getFlowId(), executionCandidates);
            } else {
                addSimpleAuthenticationExecution(processor, requiredExecution, executionCandidates);
                return true;
            }
        } else {
            // We're going through all the alternatives
            boolean anyAdded = false;

            for (AuthenticationExecutionModel execution : alternativeList) {
                // Don't add already processed executions
                if (flow.isProcessed(execution)) {
                    continue;
                }

                if (!execution.isAuthenticatorFlow()) {
                    addSimpleAuthenticationExecution(processor, execution, executionCandidates);
                    anyAdded = true;
                } else {
                    anyAdded |= addAllExecutionsFromSubflow(processor, execution.getFlowId(), executionCandidates);
                }
            }

            return anyAdded;
        }

    }
}
