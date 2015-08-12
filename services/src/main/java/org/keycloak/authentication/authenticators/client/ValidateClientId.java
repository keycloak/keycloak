package org.keycloak.authentication.authenticators.client;

import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.util.BasicAuthHelper;

/**
 * TODO: Should be removed? Or allowed just per public clients?
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ValidateClientId extends AbstractClientAuthenticator {

    public static final String PROVIDER_ID = "client-id";

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED
    };

    @Override
    public void authenticateClient(ClientAuthenticationFlowContext context) {
        ClientModel client = ClientAuthUtil.getClientFromClientId(context);
        if (client == null) {
            return;
        }

        context.setClient(client);
        context.success();
    }

    @Override
    public String getDisplayType() {
        return "Client ID Validation";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public boolean isConfigurablePerClient() {
        return false;
    }

    @Override
    public boolean requiresClient() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, ClientModel client) {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public String getHelpText() {
        return "Validates the clientId supplied as a 'client_id' form parameter or in 'Authorization: Basic' header";
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
