package org.keycloak.protocol.oidc.utils;

import java.util.Map;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizeClientUtil {

    public static ClientAuthResult authorizeClient(KeycloakSession session, EventBuilder event, RealmModel realm) {
        AuthenticationFlowModel clientAuthFlow = realm.getClientAuthenticationFlow();
        String flowId = clientAuthFlow.getId();

        AuthenticationProcessor processor = new AuthenticationProcessor();
        processor.setFlowId(flowId)
                .setConnection(session.getContext().getConnection())
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(session.getContext().getContextObject(HttpRequest.class));

        Response response = processor.authenticateClient();
        if (response != null) {
            throw new WebApplicationException(response);
        }

        ClientModel client = processor.getClient();
        if (client == null) {
            throw new ErrorResponseException("invalid_client", "Client authentication ended, but client is null", Response.Status.BAD_REQUEST);
        }

        return new ClientAuthResult(client, processor.getClientAuthAttributes());
    }

    public static class ClientAuthResult {

        private final ClientModel client;
        private final Map<String, String> clientAuthAttributes;

        private ClientAuthResult(ClientModel client, Map<String, String> clientAuthAttributes) {
            this.client = client;
            this.clientAuthAttributes = clientAuthAttributes;
        }

        public ClientModel getClient() {
            return client;
        }

        public Map<String, String> getClientAuthAttributes() {
            return clientAuthAttributes;
        }
    }

}
