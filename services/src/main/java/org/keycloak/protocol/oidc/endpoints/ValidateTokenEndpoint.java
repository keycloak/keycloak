package org.keycloak.protocol.oidc.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.RSATokenVerifier;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ValidateTokenEndpoint {

    private static final Logger logger = Logger.getLogger(ValidateTokenEndpoint.class);

    @Context
    private KeycloakSession session;

    @Context
    private ClientConnection clientConnection;

    @Context
    private UriInfo uriInfo;

    @Context
    private HttpHeaders headers;

    private TokenManager tokenManager;
    private RealmModel realm;
    private EventBuilder event;

    public ValidateTokenEndpoint(TokenManager tokenManager, RealmModel realm, EventBuilder event) {
        this.tokenManager = tokenManager;
        this.realm = realm;
        this.event = event;
    }

    /**
     * Validate encoded access token.
     *
     * @param tokenString
     * @return Unmarshalled token
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateAccessToken(@QueryParam("access_token") String tokenString) {
        checkSsl();

        event.event(EventType.VALIDATE_ACCESS_TOKEN);
        AccessToken token = null;
        try {
            token = RSATokenVerifier.verifyToken(tokenString, realm.getPublicKey(), realm.getName());
        } catch (Exception e) {
            Map<String, String> err = new HashMap<String, String>();
            err.put(OAuth2Constants.ERROR, OAuthErrorException.INVALID_GRANT);
            err.put(OAuth2Constants.ERROR_DESCRIPTION, "Token invalid");
            logger.error("Invalid token. Token verification failed.");
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(err)
                    .build();
        }
        event.user(token.getSubject()).session(token.getSessionState()).detail(Details.VALIDATE_ACCESS_TOKEN, token.getId());

        try {
            tokenManager.validateToken(session, uriInfo, clientConnection, realm, token, headers);
        } catch (OAuthErrorException e) {
            Map<String, String> error = new HashMap<String, String>();
            error.put(OAuth2Constants.ERROR, e.getError());
            if (e.getDescription() != null) error.put(OAuth2Constants.ERROR_DESCRIPTION, e.getDescription());
            event.error(Errors.INVALID_TOKEN);
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }
        event.success();

        return Response.ok(token, MediaType.APPLICATION_JSON_TYPE).build();
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException("invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

}
