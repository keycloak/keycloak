package org.keycloak.authentication;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.EventBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RequiredActionContextResult implements RequiredActionContext {
    protected UserSessionModel userSession;
    protected ClientSessionModel clientSession;
    protected RealmModel realm;
    protected EventBuilder eventBuilder;
    protected KeycloakSession session;
    protected Status status;
    protected Response challenge;
    protected HttpRequest httpRequest;
    protected UserModel user;
    protected RequiredActionFactory factory;

    public RequiredActionContextResult(UserSessionModel userSession, ClientSessionModel clientSession,
                                       RealmModel realm, EventBuilder eventBuilder, KeycloakSession session,
                                       HttpRequest httpRequest,
                                       UserModel user, RequiredActionFactory factory) {
        this.userSession = userSession;
        this.clientSession = clientSession;
        this.realm = realm;
        this.eventBuilder = eventBuilder;
        this.session = session;
        this.httpRequest = httpRequest;
        this.user = user;
        this.factory = factory;
    }

    @Override
    public EventBuilder getEvent() {
        return eventBuilder;
    }

    @Override
    public UserModel getUser() {
        return user;
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public ClientSessionModel getClientSession() {
        return clientSession;
    }

    @Override
    public UserSessionModel getUserSession() {
        return userSession;
    }

    @Override
    public ClientConnection getConnection() {
        return session.getContext().getConnection();
    }

    @Override
    public UriInfo getUriInfo() {
        return session.getContext().getUri();
    }

    @Override
    public KeycloakSession getSession() {
        return session;
    }

    @Override
    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    @Override
    public String generateAccessCode(String action) {
        ClientSessionCode code = new ClientSessionCode(getRealm(), getClientSession());
        code.setAction(action);
        return code.getCode();
    }

    @Override
    public Status getStatus() {
        return status;
    }

    @Override
    public void challenge(Response response) {
        status = Status.CHALLENGE;
        challenge = response;

    }

    @Override
    public void failure() {
        status = Status.FAILURE;
    }

    @Override
    public void success() {
        status = Status.SUCCESS;

    }

    @Override
    public void ignore() {
        status = Status.IGNORE;
    }

    @Override
    public URI getActionUrl(String code) {
        return LoginActionsService.requiredActionProcessor(getUriInfo())
                .queryParam(OAuth2Constants.CODE, code)
                .queryParam("action", factory.getId())
                .build(getRealm().getName());
    }

    @Override
    public URI getActionUrl() {
        String accessCode = generateAccessCode(factory.getId());
        return getActionUrl(accessCode);

    }

    @Override
    public LoginFormsProvider form() {
        String accessCode = generateAccessCode(factory.getId());
        URI action = getActionUrl(accessCode);
        LoginFormsProvider provider = getSession().getProvider(LoginFormsProvider.class)
                .setUser(getUser())
                .setActionUri(action)
                .setClientSessionCode(accessCode);
        return provider;
    }


    @Override
    public Response getChallenge() {
        return challenge;
    }
}
