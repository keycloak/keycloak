package org.keycloak.authentication.authenticators.client;

import jakarta.ws.rs.core.MultivaluedMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.authentication.ClientAuthenticationFlowContextSupplier;
import org.keycloak.events.Details;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.representations.JsonWebToken;

public class ClientAssertionState {

    private static final Supplier SUPPLIER = new Supplier();

    private ClientModel client;
    private final String clientAssertionType;
    private final String clientAssertion;
    private final JWSInput jws;
    private final JsonWebToken token;

    public ClientAssertionState(ClientModel client, String clientAssertionType, String clientAssertion, JWSInput jws, JsonWebToken token) {
        this.client = client;
        this.clientAssertionType = clientAssertionType;
        this.clientAssertion = clientAssertion;
        this.jws = jws;
        this.token = token;
    }

    public void setClient(ClientModel client) {
        this.client = client;
    }

    public String getClientAssertionType() {
        return clientAssertionType;
    }

    public String getClientAssertion() {
        return clientAssertion;
    }

    public JWSInput getJws() {
        return jws;
    }

    public JsonWebToken getToken() {
        return token;
    }

    public ClientModel getClient() {
        return client;
    }

    public static ClientAuthenticationFlowContextSupplier<ClientAssertionState> supplier() {
        return SUPPLIER;
    }

    private static class Supplier implements ClientAuthenticationFlowContextSupplier<ClientAssertionState> {

        @Override
        public ClientAssertionState get(ClientAuthenticationFlowContext context) throws JWSInputException {
            MultivaluedMap<String, String> params = context.getHttpRequest().getDecodedFormParameters();

            String clientAssertionType = params.getFirst(OAuth2Constants.CLIENT_ASSERTION_TYPE);
            String clientAssertion = params.getFirst(OAuth2Constants.CLIENT_ASSERTION);

            JWSInput jws = null;
            JsonWebToken token = null;

            ClientModel client = null;

            if (clientAssertion != null) {
                jws = new JWSInput(clientAssertion);
                token = jws.readJsonContent(JsonWebToken.class);

                var event = context.getEvent();
                event.detail(Details.CLIENT_ASSERTION_ID, token.getId());
                event.detail(Details.CLIENT_ASSERTION_ISSUER, token.getIssuer());
                event.detail(Details.CLIENT_ASSERTION_SUB, token.getSubject());

                if (token.getSubject() != null) {
                    client = context.getRealm().getClientByClientId(token.getSubject());
                }
            }

            return new ClientAssertionState(client, clientAssertionType, clientAssertion, jws, token);
        }

    }

}
