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

package org.keycloak.authentication;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ServicesLogger;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DefaultAuthenticationFlow implements AuthenticationFlow {
    private static final Logger logger = Logger.getLogger(DefaultAuthenticationFlow.class);
    Response alternativeChallenge = null;
    AuthenticationExecutionModel challengedAlternativeExecution = null;
    boolean alternativeSuccessful = false;
    List<AuthenticationExecutionModel> executions;
    Iterator<AuthenticationExecutionModel> executionIterator;
    AuthenticationProcessor processor;
    AuthenticationFlowModel flow;

    public DefaultAuthenticationFlow(AuthenticationProcessor processor, AuthenticationFlowModel flow) {
        this.processor = processor;
        this.flow = flow;
        this.executions = processor.getRealm().getAuthenticationExecutions(flow.getId());
        this.executionIterator = executions.iterator();
    }

    protected boolean isProcessed(AuthenticationExecutionModel model) {
        if (model.isDisabled()) return true;
        AuthenticationSessionModel.ExecutionStatus status = processor.getAuthenticationSession().getExecutionStatus().get(model.getId());
        if (status == null) return false;
        return status == AuthenticationSessionModel.ExecutionStatus.SUCCESS || status == AuthenticationSessionModel.ExecutionStatus.SKIPPED
                || status == AuthenticationSessionModel.ExecutionStatus.ATTEMPTED
                || status == AuthenticationSessionModel.ExecutionStatus.SETUP_REQUIRED;
    }


    @Override
    public Response processAction(String actionExecution) {
        logger.debugv("processAction: {0}", actionExecution);
        while (executionIterator.hasNext()) {
            AuthenticationExecutionModel model = executionIterator.next();
            logger.debugv("check: {0} requirement: {1}", model.getAuthenticator(), model.getRequirement().toString());
            if (isProcessed(model)) {
                logger.debug("execution is processed");
                if (!alternativeSuccessful && model.isAlternative() && processor.isSuccessful(model))
                    alternativeSuccessful = true;
                continue;
            }
            if (model.isAuthenticatorFlow()) {
                AuthenticationFlow authenticationFlow = processor.createFlowExecution(model.getFlowId(), model);
                Response flowChallenge = authenticationFlow.processAction(actionExecution);
                if (flowChallenge == null) {
                    processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SUCCESS);
                    if (model.isAlternative()) alternativeSuccessful = true;
                    return processFlow();
                } else {
                   return flowChallenge;
                }
            } else if (model.getId().equals(actionExecution)) {
                AuthenticatorFactory factory = (AuthenticatorFactory) processor.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getAuthenticator());
                if (factory == null) {
                    throw new RuntimeException("Unable to find factory for AuthenticatorFactory: " + model.getAuthenticator() + " did you forget to declare it in a META-INF/services file?");
                }
                Authenticator authenticator = factory.create(processor.getSession());
                AuthenticationProcessor.Result result = processor.createAuthenticatorContext(model, authenticator, executions);
                logger.debugv("action: {0}", model.getAuthenticator());
                authenticator.action(result);
                Response response = processResult(result, true);
                if (response == null) {
                    processor.getAuthenticationSession().removeAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION);
                    return processFlow();
                } else return response;
            }
        }
        throw new AuthenticationFlowException("action is not in current execution", AuthenticationFlowError.INTERNAL_ERROR);
    }

    @Override
    public Response processFlow() {
        logger.debug("processFlow");
        while (executionIterator.hasNext()) {
            AuthenticationExecutionModel model = executionIterator.next();
            logger.debugv("check execution: {0} requirement: {1}", model.getAuthenticator(), model.getRequirement().toString());

            if (isProcessed(model)) {
                logger.debug("execution is processed");
                if (!alternativeSuccessful && model.isAlternative() && processor.isSuccessful(model))
                    alternativeSuccessful = true;
                continue;
            }
            if (model.isAlternative() && alternativeSuccessful) {
                logger.debug("Skip alternative execution");
                processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SKIPPED);
                continue;
            }
            if (model.isAuthenticatorFlow()) {
                logger.debug("execution is flow");
                AuthenticationFlow authenticationFlow = processor.createFlowExecution(model.getFlowId(), model);

                Response flowChallenge = null;
                try {
                    flowChallenge = authenticationFlow.processFlow();
                } catch (AuthenticationFlowException afe) {
                    if (model.isAlternative()) {
                        logger.debug("Thrown exception in alternative Subflow. Ignoring Subflow");
                        processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
                        continue;
                    } else {
                        throw afe;
                    }
                }

                if (flowChallenge == null) {
                    processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SUCCESS);
                    if (model.isAlternative()) alternativeSuccessful = true;
                    continue;
                } else {
                    if (model.isAlternative()) {
                        alternativeChallenge = flowChallenge;
                        challengedAlternativeExecution = model;
                    } else if (model.isRequired()) {
                        processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                        return flowChallenge;
                    } else if (model.isOptional()) {
                        processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SKIPPED);
                        continue;
                    } else {
                        processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SKIPPED);
                        continue;
                    }
                    return flowChallenge;
                }
            }

            AuthenticatorFactory factory = (AuthenticatorFactory) processor.getSession().getKeycloakSessionFactory().getProviderFactory(Authenticator.class, model.getAuthenticator());
            if (factory == null) {
                throw new RuntimeException("Unable to find factory for AuthenticatorFactory: " + model.getAuthenticator() + " did you forget to declare it in a META-INF/services file?");
            }
            Authenticator authenticator = factory.create(processor.getSession());
            logger.debugv("authenticator: {0}", factory.getId());
            UserModel authUser = processor.getAuthenticationSession().getAuthenticatedUser();

            if (authenticator.requiresUser() && authUser == null) {
                if (alternativeChallenge != null) {
                    processor.getAuthenticationSession().setExecutionStatus(challengedAlternativeExecution.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                    return alternativeChallenge;
                }
                throw new AuthenticationFlowException("authenticator: " + factory.getId(), AuthenticationFlowError.UNKNOWN_USER);
            }
            boolean configuredFor = false;
            if (authenticator.requiresUser() && authUser != null) {
                configuredFor = authenticator.configuredFor(processor.getSession(), processor.getRealm(), authUser);
                if (!configuredFor) {
                    if (model.isRequired()) {
                        if (factory.isUserSetupAllowed()) {
                            logger.debugv("authenticator SETUP_REQUIRED: {0}", factory.getId());
                            processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SETUP_REQUIRED);
                            authenticator.setRequiredActions(processor.getSession(), processor.getRealm(), processor.getAuthenticationSession().getAuthenticatedUser());
                            continue;
                        } else {
                            throw new AuthenticationFlowException(AuthenticationFlowError.CREDENTIAL_SETUP_REQUIRED);
                        }
                    } else if (model.isOptional()) {
                        processor.getAuthenticationSession().setExecutionStatus(model.getId(), AuthenticationSessionModel.ExecutionStatus.SKIPPED);
                        continue;
                    }
                }
            }
            // skip if action as successful already
//            Response redirect = processor.checkWasSuccessfulBrowserAction();
//            if (redirect != null) return redirect;

            AuthenticationProcessor.Result context = processor.createAuthenticatorContext(model, authenticator, executions);
            logger.debugv("invoke authenticator.authenticate: {0}", factory.getId());
            authenticator.authenticate(context);
            Response response = processResult(context, false);
            if (response != null) return response;
        }
        return null;
    }


    public Response processResult(AuthenticationProcessor.Result result, boolean isAction) {
        AuthenticationExecutionModel execution = result.getExecution();
        FlowStatus status = result.getStatus();
        switch (status) {
            case SUCCESS:
                logger.debugv("authenticator SUCCESS: {0}", execution.getAuthenticator());
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.SUCCESS);
                if (execution.isAlternative()) alternativeSuccessful = true;
                return null;
            case FAILED:
                logger.debugv("authenticator FAILED: {0}", execution.getAuthenticator());
                processor.logFailure();
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.FAILED);
                if (result.getChallenge() != null) {
                    return sendChallenge(result, execution);
                }
                throw new AuthenticationFlowException(result.getError());
            case FORK:
                logger.debugv("reset browser login from authenticator: {0}", execution.getAuthenticator());
                processor.getAuthenticationSession().setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution.getId());
                throw new ForkFlowException(result.getSuccessMessage(), result.getErrorMessage());
            case FORCE_CHALLENGE:
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return sendChallenge(result, execution);
            case CHALLENGE:
                logger.debugv("authenticator CHALLENGE: {0}", execution.getAuthenticator());
                if (execution.isRequired()) {
                    processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                    return sendChallenge(result, execution);
                }
                UserModel authenticatedUser = processor.getAuthenticationSession().getAuthenticatedUser();
                if (execution.isOptional() && authenticatedUser != null && result.getAuthenticator().configuredFor(processor.getSession(), processor.getRealm(), authenticatedUser)) {
                    processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                    return sendChallenge(result, execution);
                }
                if (execution.isAlternative()) {
                    alternativeChallenge = result.getChallenge();
                    challengedAlternativeExecution = execution;
                } else {
                    processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.SKIPPED);
                }
                return null;
            case FAILURE_CHALLENGE:
                logger.debugv("authenticator FAILURE_CHALLENGE: {0}", execution.getAuthenticator());
                processor.logFailure();
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.CHALLENGED);
                return sendChallenge(result, execution);
            case ATTEMPTED:
                logger.debugv("authenticator ATTEMPTED: {0}", execution.getAuthenticator());
                if (execution.getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                    throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_CREDENTIALS);
                }
                processor.getAuthenticationSession().setExecutionStatus(execution.getId(), AuthenticationSessionModel.ExecutionStatus.ATTEMPTED);
                return null;
            case FLOW_RESET:
                processor.resetFlow();
                return processor.authenticate();
            default:
                logger.debugv("authenticator INTERNAL_ERROR: {0}", execution.getAuthenticator());
                ServicesLogger.LOGGER.unknownResultStatus();
                throw new AuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    public Response sendChallenge(AuthenticationProcessor.Result result, AuthenticationExecutionModel execution) {
        processor.getAuthenticationSession().setAuthNote(AuthenticationProcessor.CURRENT_AUTHENTICATION_EXECUTION, execution.getId());
        return result.getChallenge();
    }


}
