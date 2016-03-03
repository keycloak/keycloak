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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.ServicesLogger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAuthenticationFlow implements AuthenticationFlow {

    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;

    Response alternativeChallenge = null;
    AuthenticationProcessor processor;
    AuthenticationFlowModel flow;

    public ClientAuthenticationFlow(AuthenticationProcessor processor, AuthenticationFlowModel flow) {
        this.processor = processor;
        this.flow = flow;
    }

    @Override
    public Response processAction(String actionExecution) {
        throw new IllegalStateException("Not supposed to be invoked");
    }

    @Override
    public Response processFlow() {
        List<AuthenticationExecutionModel> executions = findExecutionsToRun();

        for (AuthenticationExecutionModel model : executions) {
            ClientAuthenticatorFactory factory = (ClientAuthenticatorFactory) processor.getSession().getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, model.getAuthenticator());
            if (factory == null) {
                throw new AuthenticationFlowException("Could not find ClientAuthenticatorFactory for: " + model.getAuthenticator(), AuthenticationFlowError.INTERNAL_ERROR);
            }
            ClientAuthenticator authenticator = factory.create();
            logger.debugv("client authenticator: {0}", factory.getId());

            AuthenticationProcessor.Result context = processor.createClientAuthenticatorContext(model, authenticator, executions);
            authenticator.authenticateClient(context);

            ClientModel client = processor.getClient();
            if (client != null) {
                String expectedClientAuthType = client.getClientAuthenticatorType();

                // Fallback to secret just in case (for backwards compatibility)
                if (expectedClientAuthType == null) {
                    expectedClientAuthType = KeycloakModelUtils.getDefaultClientAuthenticatorType();
                    logger.authMethodFallback(client.getClientId(), expectedClientAuthType);
                }

                // Check if client authentication matches
                if (factory.getId().equals(expectedClientAuthType)) {
                    Response response = processResult(context);
                    if (response != null) return response;

                    if (!context.getStatus().equals(FlowStatus.SUCCESS)) {
                        throw new AuthenticationFlowException("Expected success, but for an unknown reason the status was " + context.getStatus(), AuthenticationFlowError.INTERNAL_ERROR);
                    }

                    logger.debugv("Client {0} authenticated by {1}", client.getClientId(), factory.getId());
                    processor.getEvent().detail(Details.CLIENT_AUTH_METHOD, factory.getId());
                    return null;
                }
            }
        }

        // Check if any alternative challenge was identified
        if (alternativeChallenge != null) {
            processor.getEvent().error(Errors.INVALID_CLIENT);
            return alternativeChallenge;
        }
        throw new AuthenticationFlowException("Client was not identified by any client authenticator", AuthenticationFlowError.UNKNOWN_CLIENT);
    }

    protected List<AuthenticationExecutionModel> findExecutionsToRun() {
        List<AuthenticationExecutionModel> executions = processor.getRealm().getAuthenticationExecutions(flow.getId());
        List<AuthenticationExecutionModel> executionsToRun = new ArrayList<>();

        for (AuthenticationExecutionModel execution : executions) {
            if (execution.isRequired()) {
                executionsToRun = Arrays.asList(execution);
                break;
            }

            if (execution.isAlternative()) {
                executionsToRun.add(execution);
            }
        }

        if (logger.isTraceEnabled()) {
            List<String> exIds = new ArrayList<>();
            for (AuthenticationExecutionModel execution : executionsToRun) {
                exIds.add(execution.getId());
            }
            logger.tracef("Using executions for client authentication: %s", exIds.toString());
        }

        return executionsToRun;
    }

    protected Response processResult(AuthenticationProcessor.Result result) {
        AuthenticationExecutionModel execution = result.getExecution();
        FlowStatus status = result.getStatus();

        logger.debugv("client authenticator {0}: {1}", status.toString(), execution.getAuthenticator());

        if (status == FlowStatus.SUCCESS) {
            return null;
        }

        if (status == FlowStatus.FAILED) {
            if (result.getChallenge() != null) {
                return sendChallenge(result, execution);
            } else {
                throw new AuthenticationFlowException(result.getError());
            }
        } else if (status == FlowStatus.FORCE_CHALLENGE) {
            return sendChallenge(result, execution);
        } else if (status == FlowStatus.CHALLENGE) {

            // Make sure the first priority alternative challenge is used
            if (alternativeChallenge == null) {
                alternativeChallenge = result.getChallenge();
            }
            return sendChallenge(result, execution);
        } else if (status == FlowStatus.FAILURE_CHALLENGE) {
            return sendChallenge(result, execution);
        } else {
            logger.unknownResultStatus();
            throw new AuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    public Response sendChallenge(AuthenticationProcessor.Result result, AuthenticationExecutionModel execution) {
        logger.debugv("client authenticator: sending challenge for authentication execution {0}", execution.getAuthenticator());

        if (result.getError() != null) {
            String errorAsString = result.getError().toString().toLowerCase();
            result.getEvent().error(errorAsString);
        } else {
            if (result.getClient() == null) {
                result.getEvent().error(Errors.INVALID_CLIENT);
            } else {
                result.getEvent().error(Errors.INVALID_CLIENT_CREDENTIALS);
            }
        }

        return result.getChallenge();
    }
}
