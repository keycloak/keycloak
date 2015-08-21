package org.keycloak.authentication;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAuthenticationFlow implements AuthenticationFlow {

    Response alternativeChallenge = null;
    boolean alternativeSuccessful = false;
    List<AuthenticationExecutionModel> executions;
    Iterator<AuthenticationExecutionModel> executionIterator;
    AuthenticationProcessor processor;
    AuthenticationFlowModel flow;

    private List<String> successAuthenticators = new LinkedList<>();

    public ClientAuthenticationFlow(AuthenticationProcessor processor, AuthenticationFlowModel flow) {
        this.processor = processor;
        this.flow = flow;
        this.executions = processor.getRealm().getAuthenticationExecutions(flow.getId());
        this.executionIterator = executions.iterator();
    }

    @Override
    public Response processAction(String actionExecution) {
        throw new IllegalStateException("Not supposed to be invoked");
    }

    @Override
    public Response processFlow() {
        while (executionIterator.hasNext()) {
            AuthenticationExecutionModel model = executionIterator.next();

            if (model.isDisabled()) {
                continue;
            }

            if (model.isAlternative() && alternativeSuccessful) {
                continue;
            }

            if (model.isAuthenticatorFlow()) {
                AuthenticationFlow authenticationFlow;
                authenticationFlow = processor.createFlowExecution(model.getFlowId(), model);

                Response flowChallenge = authenticationFlow.processFlow();
                if (flowChallenge == null) {
                    if (model.isAlternative()) alternativeSuccessful = true;
                    continue;
                } else {
                    if (model.isAlternative()) {
                        alternativeChallenge = flowChallenge;
                    } else if (model.isRequired()) {
                        return flowChallenge;
                    } else {
                        continue;
                    }
                    return flowChallenge;
                }
            }

            ClientAuthenticatorFactory factory = (ClientAuthenticatorFactory) processor.getSession().getKeycloakSessionFactory().getProviderFactory(ClientAuthenticator.class, model.getAuthenticator());
            if (factory == null) {
                throw new AuthenticationFlowException("Could not find ClientAuthenticatorFactory for: " + model.getAuthenticator(), AuthenticationFlowError.INTERNAL_ERROR);
            }
            ClientAuthenticator authenticator = factory.create();
            AuthenticationProcessor.logger.debugv("client authenticator: {0}", factory.getId());
            ClientModel authClient = processor.getClient();

            if (authenticator.requiresClient() && authClient == null) {
                // Continue if it's alternative or optional flow
                if (model.isAlternative() || model.isOptional()) {
                    AuthenticationProcessor.logger.debugv("client authenticator: {0} requires client, but client not available. Skipping", factory.getId());
                    continue;
                }

                if (alternativeChallenge != null) {
                    return alternativeChallenge;
                }
                throw new AuthenticationFlowException("client authenticator: " + factory.getId(), AuthenticationFlowError.CLIENT_NOT_FOUND);
            }

            if (authenticator.requiresClient() && authClient != null) {
                boolean configuredFor = authenticator.configuredFor(processor.getSession(), processor.getRealm(), authClient);
                if (!configuredFor) {
                    if (model.isRequired()) {
                        throw new AuthenticationFlowException("Client setup required for authenticator " + factory.getId() + " for client " + authClient.getClientId(),
                                AuthenticationFlowError.CLIENT_CREDENTIALS_SETUP_REQUIRED);
                    } else if (model.isOptional()) {
                        continue;
                    }
                }
            }
            AuthenticationProcessor.Result context = processor.createClientAuthenticatorContext(model, authenticator, executions);
            authenticator.authenticateClient(context);
            Response response = processResult(context);
            if (response != null) return response;

            authClient = processor.getClient();
            if (authClient != null && authClient.isPublicClient()) {
                AuthenticationProcessor.logger.debugv("Public client {0} identified by {1} . Skip next client authenticators", authClient.getClientId(), factory.getId());
                logSuccessEvent();
                return null;
            }
        }

        return finishClientAuthentication();
    }


    public Response processResult(AuthenticationProcessor.Result result) {
        AuthenticationExecutionModel execution = result.getExecution();
        FlowStatus status = result.getStatus();
        if (status == FlowStatus.SUCCESS) {
            AuthenticationProcessor.logger.debugv("client authenticator SUCCESS: {0}", execution.getAuthenticator());
            if (execution.isAlternative()) alternativeSuccessful = true;
            successAuthenticators.add(execution.getAuthenticator());
            return null;
        } else if (status == FlowStatus.FAILED) {
            AuthenticationProcessor.logger.debugv("client authenticator FAILED: {0}", execution.getAuthenticator());
            if (result.getChallenge() != null) {
                return sendChallenge(result, execution);
            }
            throw new AuthenticationFlowException(result.getError());
        } else if (status == FlowStatus.FORCE_CHALLENGE) {
            return sendChallenge(result, execution);
        } else if (status == FlowStatus.CHALLENGE) {
            AuthenticationProcessor.logger.debugv("client authenticator CHALLENGE: {0}", execution.getAuthenticator());
            if (execution.isRequired()) {
                return sendChallenge(result, execution);
            }
            ClientModel client = processor.getClient();
            if (execution.isOptional() && client != null && result.getClientAuthenticator().configuredFor(processor.getSession(), processor.getRealm(), client)) {
                return sendChallenge(result, execution);
            }
            // Make sure the first priority alternative challenge is used
            if (execution.isAlternative() && alternativeChallenge == null) {
                alternativeChallenge = result.getChallenge();
            }
            return null;
        } else if (status == FlowStatus.FAILURE_CHALLENGE) {
            AuthenticationProcessor.logger.debugv("client authenticator FAILURE_CHALLENGE: {0}", execution.getAuthenticator());
            return sendChallenge(result, execution);
        } else if (status == FlowStatus.ATTEMPTED) {
            AuthenticationProcessor.logger.debugv("client authenticator ATTEMPTED: {0}", execution.getAuthenticator());
            if (execution.getRequirement() == AuthenticationExecutionModel.Requirement.REQUIRED) {
                throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS);
            }
            return null;
        } else {
            AuthenticationProcessor.logger.debugv("client authenticator INTERNAL_ERROR: {0}", execution.getAuthenticator());
            AuthenticationProcessor.logger.error("Unknown result status");
            throw new AuthenticationFlowException(AuthenticationFlowError.INTERNAL_ERROR);
        }

    }

    public Response sendChallenge(AuthenticationProcessor.Result result, AuthenticationExecutionModel execution) {
        AuthenticationProcessor.logger.debugv("client authenticator: sending challenge for authentication execution {0}", execution.getAuthenticator());

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

    private Response finishClientAuthentication() {
        if (processor.getClient() == null) {
            // Check if any alternative challenge was identified
            if (alternativeChallenge != null) {
                processor.getEvent().error(Errors.INVALID_CLIENT);
                return alternativeChallenge;
            }

            throw new AuthenticationFlowException("Client was not identified by any client authenticator", AuthenticationFlowError.UNKNOWN_CLIENT);
        }

        logSuccessEvent();
        return null;
    }

    private void logSuccessEvent() {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String authenticator : successAuthenticators) {
            if (first) {
                first = false;
            } else {
                result.append(" ");
            }
            result.append(authenticator);
        }

        processor.getEvent().detail(Details.CLIENT_AUTH_METHOD, result.toString());
    }
}
