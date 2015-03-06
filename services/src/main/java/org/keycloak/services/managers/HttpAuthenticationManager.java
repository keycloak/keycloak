package org.keycloak.services.managers;

import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.CredentialValidationOutput;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.constants.KerberosConstants;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;

/**
 * Handle HTTP authentication types requiring complex handshakes with multiple HTTP request/responses
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class HttpAuthenticationManager {

    private static final Logger logger = Logger.getLogger(HttpAuthenticationManager.class);

    private KeycloakSession session;
    private RealmModel realm;
    private UriInfo uriInfo;
    private HttpRequest request;
    private EventBuilder event;
    private ClientConnection clientConnection;
    private ClientSessionModel clientSession;

    public HttpAuthenticationManager(KeycloakSession session, ClientSessionModel clientSession, RealmModel realm, UriInfo uriInfo,
                                     HttpRequest request,
                                     ClientConnection clientConnection,
                                     EventBuilder event) {
        this.session = session;
        this.realm = realm;
        this.uriInfo = uriInfo;
        this.request = request;
        this.event = event;
        this.clientConnection = clientConnection;
        this.clientSession = clientSession;
    }


    public HttpAuthOutput spnegoAuthenticate(HttpHeaders headers) {
        boolean kerberosSupported = false;
        for (RequiredCredentialModel c : realm.getRequiredCredentials()) {
            if (c.getType().equals(CredentialRepresentation.KERBEROS)) {
                kerberosSupported = true;
            }
        }

        if (logger.isTraceEnabled()) {
            String log = kerberosSupported ? "SPNEGO authentication is supported" : "SPNEGO authentication is not supported";
            logger.trace(log);
        }

        if (!kerberosSupported) {
            return new HttpAuthOutput(null, null);
        }

        String authHeader = request.getHttpHeaders().getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Case when we don't yet have any Negotiate header
        if (authHeader == null) {
            return challengeNegotiation(null);
        }

        String[] tokens = authHeader.split(" ");
        if (tokens.length != 2) {
            logger.warn("Invalid length of tokens: " + tokens.length);
            return challengeNegotiation(null);
        } else if (!KerberosConstants.NEGOTIATE.equalsIgnoreCase(tokens[0])) {
            logger.warn("Unknown scheme " + tokens[0]);
            return challengeNegotiation(null);
        } else {
            String spnegoToken = tokens[1];
            UserCredentialModel spnegoCredential = UserCredentialModel.kerberos(spnegoToken);

            CredentialValidationOutput output = session.users().validCredentials(realm, spnegoCredential);

            if (output.getAuthStatus() == CredentialValidationOutput.Status.AUTHENTICATED) {
                return sendResponse(output.getAuthenticatedUser(), output.getState(), "spnego", headers);
            }  else {
                String spnegoResponseToken = (String) output.getState().get(KerberosConstants.RESPONSE_TOKEN);
                return challengeNegotiation(spnegoResponseToken);
            }
        }
    }


    // Send response after successful authentication
    private HttpAuthOutput sendResponse(UserModel user, Map<String, String> authState, String authMethod, HttpHeaders headers) {
        if (logger.isTraceEnabled()) {
            logger.trace("User " + user.getUsername() + " authenticated with " + authMethod);
        }

        Response response;
        if (!user.isEnabled()) {
            event.error(Errors.USER_DISABLED);
            response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, headers, Messages.ACCOUNT_DISABLED);
        } else {
            UserSessionModel userSession = session.sessions().createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), authMethod, false);

            // Propagate state (like kerberos delegation credentials etc) as attributes of userSession
            for (Map.Entry<String, String> entry : authState.entrySet()) {
                userSession.setNote(entry.getKey(), entry.getValue());
            }

            TokenManager.attachClientSession(userSession, clientSession);
            event.user(user)
                    .session(userSession)
                    .detail(Details.AUTH_METHOD, authMethod)
                    .detail(Details.USERNAME, user.getUsername());
            response = AuthenticationManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
        }

        return new HttpAuthOutput(response, null);
    }


    private HttpAuthOutput challengeNegotiation(final String negotiateToken) {
        return new HttpAuthOutput(null, new HttpAuthChallenge() {

            @Override
            public void sendChallenge(LoginFormsProvider loginFormsProvider) {
                String negotiateHeader = negotiateToken == null ? KerberosConstants.NEGOTIATE : KerberosConstants.NEGOTIATE + " " + negotiateToken;

                if (logger.isTraceEnabled()) {
                    logger.trace("Sending back " + HttpHeaders.WWW_AUTHENTICATE + ": " + negotiateHeader);
                }

                loginFormsProvider.setStatus(Response.Status.UNAUTHORIZED);
                loginFormsProvider.setResponseHeader(HttpHeaders.WWW_AUTHENTICATE, negotiateHeader);
            }

        });
    }


    public class HttpAuthOutput {

        // It's non-null if we want to immediately send response to user
        private final Response response;

        // It's non-null if challenge should be attached to rendered login form
        private final HttpAuthChallenge challenge;

        public HttpAuthOutput(Response response, HttpAuthChallenge challenge) {
            this.response = response;
            this.challenge = challenge;
        }

        public Response getResponse() {
            return response;
        }

        public HttpAuthChallenge getChallenge() {
            return challenge;
        }
    }


    public interface HttpAuthChallenge {

        void sendChallenge(LoginFormsProvider loginFormsProvider);

    }

}
