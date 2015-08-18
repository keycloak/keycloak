package org.keycloak.authentication.authenticators.client;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.models.ClientModel;
import org.keycloak.util.BasicAuthHelper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientAuthUtil {


    public static Response errorResponse(int status, String error, String errorDescription) {
        Map<String, String> e = new HashMap<String, String>();
        e.put(OAuth2Constants.ERROR, error);
        if (errorDescription != null) {
            e.put(OAuth2Constants.ERROR_DESCRIPTION, errorDescription);
        }
        return Response.status(status).entity(e).type(MediaType.APPLICATION_JSON_TYPE).build();
    }


    // Return client either from client_id parameter or from "username" send in "Authorization: Basic" header.
    public static ClientModel getClientFromClientId(ClientAuthenticationFlowContext context) {
        String client_id = null;
        String authorizationHeader = context.getHttpRequest().getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

        if (authorizationHeader != null) {
            String[] usernameSecret = BasicAuthHelper.parseHeader(authorizationHeader);
            if (usernameSecret != null) {
                client_id = usernameSecret[0];
            } else {

                // Don't send 401 if client_id parameter was sent in request. For example IE may automatically send "Authorization: Negotiate" in XHR requests even for public clients
                if (!formData.containsKey(OAuth2Constants.CLIENT_ID)) {
                    Response challengeResponse = Response.status(Response.Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + context.getRealm().getName() + "\"").build();
                    context.challenge(challengeResponse);
                    return null;
                }
            }
        }

        if (client_id == null) {
            client_id = formData.getFirst(OAuth2Constants.CLIENT_ID);
        }

        if (client_id == null) {
            Response challengeResponse = ClientAuthUtil.errorResponse(Response.Status.BAD_REQUEST.getStatusCode(), "invalid_client", "Missing client_id parameter");
            context.challenge(challengeResponse);
            return null;
        }

        context.getEvent().client(client_id);

        ClientModel client = context.getRealm().getClientByClientId(client_id);
        if (client == null) {
            context.failure(AuthenticationFlowError.CLIENT_NOT_FOUND, null);
            return null;
        }

        if (!client.isEnabled()) {
            context.failure(AuthenticationFlowError.CLIENT_DISABLED, null);
            return null;
        }

        return client;
    }
}
