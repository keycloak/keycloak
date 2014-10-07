package org.keycloak.protocol.saml;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OpenIDConnectService;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.resources.flows.Flows;
import org.picketlink.common.constants.GeneralConstants;
import org.picketlink.identity.federation.core.saml.v2.common.SAMLDocumentHolder;
import org.picketlink.identity.federation.saml.v2.SAML2Object;
import org.picketlink.identity.federation.saml.v2.protocol.AuthnRequestType;
import org.picketlink.identity.federation.saml.v2.protocol.LogoutRequestType;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource class for the oauth/openid connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlService {

    protected static final Logger logger = Logger.getLogger(SamlService.class);

    protected RealmModel realm;
    private EventBuilder event;
    protected AuthenticationManager authManager;

    @Context
    protected Providers providers;
    @Context
    protected SecurityContext securityContext;
    @Context
    protected UriInfo uriInfo;
    @Context
    protected HttpHeaders headers;
    @Context
    protected HttpRequest request;
    @Context
    protected HttpResponse response;
    @Context
    protected KeycloakSession session;
    @Context
    protected ClientConnection clientConnection;

    /*
    @Context
    protected ResourceContext resourceContext;
    */

    public SamlService(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        this.realm = realm;
        this.event = event;
        this.authManager = authManager;
    }

    /**
     */
    @Path("POST")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        if (!checkSsl()) {
            event.event(EventType.LOGIN_ERROR);
            event.error(Errors.SSL_REQUIRED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "HTTPS required");
        }
        if (!realm.isEnabled()) {
            event.event(EventType.LOGIN_ERROR);
            event.error(Errors.REALM_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Realm not enabled");
        }

        if (samlRequest == null && samlResponse == null) {
            event.event(EventType.LOGIN_ERROR);
            event.error(Errors.INVALID_TOKEN);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");

        }

        if (samlRequest != null) return handleSamlRequest(samlRequest, relayState);
        else  return handleSamlResponse(samlResponse, relayState);
    }

    protected Response handleSamlResponse(String samleResponse, String relayState) {
        event.event(EventType.LOGIN_ERROR);
        event.error(Errors.INVALID_TOKEN);
        return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");
    }


    protected Response handleSamlRequest(String samlRequest, String relayState) {
        SAMLDocumentHolder documentHolder = SAMLRequestParser.parsePostBinding(samlRequest);
        if (documentHolder == null) {
            event.event(EventType.LOGIN_ERROR);
            event.error(Errors.INVALID_TOKEN);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");
        }

        SAML2Object samlObject = documentHolder.getSamlObject();

        if (samlObject instanceof AuthnRequestType) {
            event.event(EventType.LOGIN);
            // Get the SAML Request Message
            AuthnRequestType requestAbstractType = (AuthnRequestType) samlObject;
            return loginRequest(relayState, requestAbstractType);
        } else if (samlObject instanceof LogoutRequestType) {
            event.event(EventType.LOGOUT);
            LogoutRequestType requestAbstractType = (LogoutRequestType) samlObject;
            return logoutRequest(relayState, requestAbstractType);

        } else {
            event.event(EventType.LOGIN_ERROR);
            event.error(Errors.INVALID_TOKEN);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid Request");
        }
    }

    protected Response loginRequest(String relayState, AuthnRequestType requestAbstractType) {
        String issuer = requestAbstractType.getIssuer().getValue();
        ClientModel client = realm.findClient(issuer);

        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Unknown login requester.");
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Login requester not enabled.");
        }
        if ((client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Bearer-only applications are not allowed to initiate browser login");
        }
        if (client.isDirectGrantsOnly()) {
            event.error(Errors.NOT_ALLOWED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "direct-grants-only clients are not allowed to initiate browser login");
        }

        URI redirectUri = requestAbstractType.getAssertionConsumerServiceURL();
        String redirect = OpenIDConnectService.verifyRedirectUri(uriInfo, redirectUri.toString(), realm, client);

        if (redirect == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid redirect_uri.");
        }


        ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(SamlLogin.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirect);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE);
        clientSession.setNote(SamlLogin.SAML_BINDING, SamlLogin.SAML_POST_BINDING);
        clientSession.setNote(GeneralConstants.RELAY_STATE, relayState);
        clientSession.setNote("REQUEST_ID", requestAbstractType.getID());

        Response response = authManager.checkNonFormAuthentication(session, clientSession, realm, uriInfo, request, clientConnection, headers, event);
        if (response != null) return response;

        LoginFormsProvider forms = Flows.forms(session, realm, clientSession.getClient(), uriInfo)
                .setClientSessionCode(new ClientSessionCode(realm, clientSession).getCode());

        String rememberMeUsername = null;
        if (realm.isRememberMe()) {
            Cookie rememberMeCookie = headers.getCookies().get(AuthenticationManager.KEYCLOAK_REMEMBER_ME);
            if (rememberMeCookie != null && !"".equals(rememberMeCookie.getValue())) {
                rememberMeUsername = rememberMeCookie.getValue();
            }
        }

        if (rememberMeUsername != null) {
            MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();
            formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
            formData.add("rememberMe", "on");

            forms.setFormData(formData);
        }

        return forms.createLogin();
    }

    protected Response logoutRequest(String relayState, LogoutRequestType requestAbstractType) {
        String issuer = requestAbstractType.getIssuer().getValue();
        ClientModel client = realm.findClient(issuer);

        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Unknown login requester.");
        }

        if (!client.isEnabled()) {
            event.error(Errors.CLIENT_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Login requester not enabled.");
        }
        if ((client instanceof ApplicationModel) && ((ApplicationModel)client).isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Bearer-only applications are not allowed to initiate browser login");
        }
        if (client.isDirectGrantsOnly()) {
            event.error(Errors.NOT_ALLOWED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "direct-grants-only clients are not allowed to initiate browser login");
        }

        // authenticate identity cookie, but ignore an access token timeout as we're logging out anyways.
        AuthenticationManager.AuthResult authResult = authManager.authenticateIdentityCookie(session, realm, uriInfo, clientConnection, headers, false);
        if (authResult != null) {
            logout(authResult.getSession());
        }

        String redirectUri = null;

        if (client instanceof ApplicationModel) {
            redirectUri = ((ApplicationModel)client).getBaseUrl();
        }

        if (redirectUri != null) {
            String validatedRedirect = OpenIDConnectService.verifyRedirectUri(uriInfo, redirectUri, realm, client);;
            if (validatedRedirect == null) {
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid redirect uri.");
            }
            return Response.status(302).location(UriBuilder.fromUri(validatedRedirect).build()).build();
        } else {
            return Response.ok().build();
        }

    }

    private void logout(UserSessionModel userSession) {
        authManager.logout(session, realm, userSession, uriInfo, clientConnection);
        event.user(userSession.getUser()).session(userSession).success();
    }

    private boolean checkSsl() {
        if (uriInfo.getBaseUri().getScheme().equals("https")) {
            return true;
        } else {
            return !realm.getSslRequired().isRequired(clientConnection);
        }
    }
}
