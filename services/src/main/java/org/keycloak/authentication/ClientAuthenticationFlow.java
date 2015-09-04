package org.keycloak.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAuthenticationFlow implements AuthenticationFlow {

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
            AuthenticationProcessor.logger.debugv("client authenticator: {0}", factory.getId());

            AuthenticationProcessor.Result context = processor.createClientAuthenticatorContext(model, authenticator, executions);
            authenticator.authenticateClient(context);
            Response response = processResult(context);
            if (response != null) return response;

            ClientModel client = processor.getClient();
            if (client != null) {

                String expectedClientAuthType = client.getClientAuthenticatorType();

                // Fallback to secret just in case (for backwards compatibility)
                if (expectedClientAuthType == null) {
                    expectedClientAuthType = KeycloakModelUtils.getDefaultClientAuthenticatorType();
                    AuthenticationProcessor.logger.warnv("Client {0} doesn't have have authentication method configured. Fallback to {1}", client.getClientId(), expectedClientAuthType);
                }

                // Check if client authentication matches
                if (factory.getId().equals(expectedClientAuthType)) {
                    AuthenticationProcessor.logger.debugv("Client {0} authenticated by {1}", client.getClientId(), factory.getId());
                    processor.getEvent().detail(Details.CLIENT_AUTH_METHOD, factory.getId());
                    return null;
                } else {
                    throw new AuthenticationFlowException("Client " + client.getClientId() + " was authenticated by incorrect method " + factory.getId(),
                            AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS);
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

        if (AuthenticationProcessor.logger.isTraceEnabled()) {
            List<String> exIds = new ArrayList<>();
            for (AuthenticationExecutionModel execution : executionsToRun) {
                exIds.add(execution.getId());
            }
            AuthenticationProcessor.logger.tracef("Using executions for client authentication: %s", exIds.toString());
        }

        return executionsToRun;
    }

    protected Response processResult(AuthenticationProcessor.Result result) {
        AuthenticationExecutionModel execution = result.getExecution();
        FlowStatus status = result.getStatus();

        AuthenticationProcessor.logger.debugv("client authenticator {0}: {1}", status.toString(), execution.getAuthenticator());

        if (status == FlowStatus.SUCCESS) {
            return null;
        } else if (status == FlowStatus.FAILED) {
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
            return null;
        } else if (status == FlowStatus.FAILURE_CHALLENGE) {
            return sendChallenge(result, execution);
        } else if (status == FlowStatus.ATTEMPTED) {
            return null;
        } else {
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
}
