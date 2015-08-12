package org.keycloak.authentication.authenticators.client;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.BasicAuthHelper;

/**
 * Validates client based on "client_id" and "client_secret" sent either in request parameters or in "Authorization: Basic" header
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientIdAndSecretAuthenticator extends AbstractClientAuthenticator {

    protected static Logger logger = Logger.getLogger(ClientIdAndSecretAuthenticator.class);

    public static final String PROVIDER_ID = "client-secret";

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        ClientModel client = ClientAuthUtil.getClientFromClientId(context);
        if (client == null) {
            return;
        } else {
            context.setClient(client);
        }

        // Skip client_secret validation for public client
        if (client.isPublicClient()) {
            context.success();
            return;
        }

        String clientSecret = getClientSecret(context);

        if (clientSecret == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Client secret not provided in request");
            context.challenge(challengeResponse);
            return;
        }

        if (client.getSecret() == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Client secret setup required for client " + client.getClientId());
            context.challenge(challengeResponse);
            return;
        }

        if (!client.validateSecret(clientSecret)) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "unauthorized_client", "Invalid client secret");
            context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
            return;
        }

        context.success();
    }

    protected String getClientSecret(ClientAuthenticationFlowContext context) {
        String clientSecret = null;
        String authorizationHeader = context.getHttpRequest().getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (authorizationHeader != null) {
            String[] usernameSecret = BasicAuthHelper.parseHeader(authorizationHeader);
            if (usernameSecret != null) {
                clientSecret = usernameSecret[1];
            }
        }

        if (clientSecret == null) {
            clientSecret = formData.getFirst("client_secret");
        }

        return clientSecret;
    }

    protected void setError(AuthenticationFlowContext context, Response challengeResponse) {
        context.getEvent().error(Errors.INVALID_CLIENT_CREDENTIALS);
        context.failure(AuthenticationFlowError.INVALID_CLIENT_CREDENTIALS, challengeResponse);
    }

    @Override
    public String getDisplayType() {
        return "Client Id and Secret";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isConfigurablePerClient() {
        return true;
    }

    @Override
    public boolean requiresClient() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, ClientModel client) {
        return client.getSecret() != null;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates client based on 'client_id' and 'client_secret' sent either in request parameters or in 'Authorization: Basic' header";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return new LinkedList<>();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
