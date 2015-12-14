package org.keycloak.protocol;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.ClientConnection;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.spi.RealmResourceProvider;

import javax.ws.rs.POST;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by fabricio on 11/12/2015.
 */
public class ForgotPasswordProvider implements RealmResourceProvider {

    private RealmModel realm;
    private KeycloakSession keycloakSession;

    private MultivaluedMap<String, String> formParams;

    @Context
    private KeycloakSession session;

    @Context
    private HttpRequest request;

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    @Context
    private ClientConnection clientConnection;

    private EventBuilder event;

    public ForgotPasswordProvider(RealmModel realm, KeycloakSession keycloakSession) {
        this.realm = realm;
        this.keycloakSession = keycloakSession;
    }

    public ForgotPasswordProvider(KeycloakSession session) {
        this.keycloakSession = session;
    }

    @Override
    public Object getResource(String pathName) {
        if (pathName.equals("forgot-password-email"))
            return this;
        return null;
    }

    @Override
    public void close() {

    }

    @POST
    public Response build() {
        // Get form data
        formParams = request.getDecodedFormParameters();

        // Do some basic checks
        checkSsl();
        checkRealm();

        String username = formParams.getFirst("username");
        UserModel user = session.users().getUserByUsername(username, session.realms().getRealmByName(realm.getName()));
        if (user == null && username.contains("@")) {
            user = session.users().getUserByEmail(username, realm);
        }

        // Create the user session and assign the corresponding action
        ClientSessionModel clientSession = createClientSession(user, null, null);
        clientSession.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        user.addRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD);
        ClientSessionCode accessCode = new ClientSessionCode(realm, clientSession);
        accessCode.setAction(ClientSessionModel.Action.EXECUTE_ACTIONS.name());

        try {
            // Build the action url
            UriBuilder builder = Urls.executeActionsBuilder(uriInfo.getBaseUri());
            builder.queryParam(Constants.KEY, accessCode.getCode());

            String link = builder.build(realm.getName()).toString();
            long expiration = TimeUnit.SECONDS.toMinutes(realm.getAccessCodeLifespanUserAction());

            session.getProvider(EmailTemplateProvider.class).setRealm(realm).setUser(user).sendPasswordReset(link, expiration);

            // Create the event
            if (event == null) {
                event = new EventBuilder(realm, session, clientConnection);
            }
            event.event(EventType.EXECUTE_ACTIONS)
                    .user(username)
                    .client(clientSession.getClient())
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Details.EMAIL, user.getEmail())
                    .session(clientSession.getId())
                    .success();

            // Return a JSON response
            Map<String, Object> res = new HashMap<>();
            res.put("actionLink", link);
            res.put("action", UserModel.RequiredAction.UPDATE_PASSWORD);
            res.put("user", user.getUsername());
            res.put("email", user.getEmail());
            res.put("expiration", expiration);

            return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
        } catch (EmailException e) {
//            logger.error("Failed to send execute actions email", e);
            return ErrorResponse.error("Failed to send execute actions email", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private ClientSessionModel createClientSession(UserModel user, String redirectUri, String clientId) {

        if (!user.isEnabled()) {
            throw new WebApplicationException(
                    ErrorResponse.error("User is disabled", Response.Status.BAD_REQUEST));
        }

        if (redirectUri != null && clientId == null) {
            throw new WebApplicationException(
                    ErrorResponse.error("Client id missing", Response.Status.BAD_REQUEST));
        }

        if (clientId == null) {
            clientId = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
        }

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null || !client.isEnabled()) {
            throw new WebApplicationException(
                    ErrorResponse.error(clientId + " not enabled", Response.Status.BAD_REQUEST));
        }

        String redirect;
        if (redirectUri != null) {
            redirect = RedirectUtils.verifyRedirectUri(uriInfo, redirectUri, realm, client);
            if (redirect == null) {
                throw new WebApplicationException(
                        ErrorResponse.error("Invalid redirect uri.", Response.Status.BAD_REQUEST));
            }
        } else {
            redirect = Urls.accountBase(uriInfo.getBaseUri()).path("/").build(realm.getName()).toString();
        }

        UserSessionModel userSession = session.sessions().createUserSession(realm, user, user.getUsername(), clientConnection.getRemoteAddr(), "form", false, null, null);
        ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirect);
        clientSession.setUserSession(userSession);

        return clientSession;
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            throw new ErrorResponseException("invalid_request", "HTTPS required", Response.Status.FORBIDDEN);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            throw new ErrorResponseException("access_denied", "Realm not enabled", Response.Status.FORBIDDEN);
        }
    }

}