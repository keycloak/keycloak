package org.keycloak.protocol.oidc.endpoints;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.RedirectUtils;
import org.keycloak.services.ErrorPageException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.HttpAuthenticationManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.Urls;

import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationEndpoint {

    private static final Logger logger = Logger.getLogger(AuthorizationEndpoint.class);

    private enum Action {
        REGISTER, CODE
    }

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

    private final AuthenticationManager authManager;
    private final RealmModel realm;
    private final EventBuilder event;

    private ClientModel client;
    private ClientSessionModel clientSession;

    private Action action;

    private String clientId;
    private String redirectUri;
    private String redirectUriParam;
    private String responseType;
    private String state;
    private String scope;
    private String loginHint;
    private String prompt;
    private String idpHint;

    private String legacyResponseType;

    public AuthorizationEndpoint(AuthenticationManager authManager, RealmModel realm, EventBuilder event) {
        this.authManager = authManager;
        this.realm = realm;
        this.event = event;
        event.event(EventType.LOGIN);
    }

    @GET
    public Response build() {
        switch (action) {
            case REGISTER:
                return buildRegister();
            case CODE:
                return buildAuthorizationCodeAuthorizationResponse();
        }

        throw new RuntimeException("Unknown action " + action);
    }

    /**
     * @deprecated
     */
    public AuthorizationEndpoint legacy(String legacyResponseType) {
        logger.warnv("Invoking deprecated endpoint {0}", uriInfo.getRequestUri());
        this.legacyResponseType = legacyResponseType;
        return this;
    }

    public AuthorizationEndpoint register() {
        event.event(EventType.REGISTER);
        action = Action.REGISTER;

        if (!realm.isRegistrationAllowed()) {
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.REGISTRATION_NOT_ALLOWED);
        }

        return this;
    }

    public AuthorizationEndpoint init() {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

        clientId = params.getFirst(OIDCLoginProtocol.CLIENT_ID_PARAM);
        responseType = params.getFirst(OIDCLoginProtocol.RESPONSE_TYPE_PARAM);
        redirectUriParam = params.getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        state = params.getFirst(OIDCLoginProtocol.STATE_PARAM);
        scope = params.getFirst(OIDCLoginProtocol.SCOPE_PARAM);
        loginHint = params.getFirst(OIDCLoginProtocol.LOGIN_HINT_PARAM);
        prompt = params.getFirst(OIDCLoginProtocol.REDIRECT_URI_PARAM);
        idpHint = params.getFirst(AdapterConstants.KC_IDP_HINT);

        checkSsl();
        checkRealm();
        checkClient();
        checkResponseType();
        checkRedirectUri();

        createClientSession();

        return this;
    }

    private void checkSsl() {
        if (!uriInfo.getBaseUri().getScheme().equals("https") && realm.getSslRequired().isRequired(clientConnection)) {
            event.error(Errors.SSL_REQUIRED);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.HTTPS_REQUIRED);
        }
    }

    private void checkRealm() {
        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.REALM_NOT_ENABLED);
        }
    }

    private void checkClient() {
        if (clientId == null) {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.MISSING_PARAMETER, OIDCLoginProtocol.CLIENT_ID_PARAM );
        }

        event.client(clientId);

        client = realm.findClient(clientId);
        if (client == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.CLIENT_NOT_FOUND );
        }

        if ((client instanceof ApplicationModel) && ((ApplicationModel) client).isBearerOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.BEARER_ONLY );
        }

        if (client.isDirectGrantsOnly()) {
            event.error(Errors.NOT_ALLOWED);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.DIRECT_GRANTS_ONLY);
        }
    }

    private void checkResponseType() {
        if (responseType == null) {
            if (legacyResponseType != null) {
                responseType = legacyResponseType;
            } else {
                event.error(Errors.INVALID_REQUEST);
                throw new ErrorPageException(session, realm, uriInfo, headers, Messages.MISSING_PARAMETER, OIDCLoginProtocol.RESPONSE_TYPE_PARAM );
            }
        }

        event.detail(Details.RESPONSE_TYPE, responseType);

        if (responseType.equals(OAuth2Constants.CODE)) {
            action = Action.CODE;
        } else {
            event.error(Errors.INVALID_REQUEST);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.INVALID_PARAMETER, OIDCLoginProtocol.RESPONSE_TYPE_PARAM );
        }
    }

    private void checkRedirectUri() {
        event.detail(Details.REDIRECT_URI, redirectUriParam);

        redirectUri = RedirectUtils.verifyRedirectUri(uriInfo, redirectUriParam, realm, client);
        if (redirectUri == null) {
            event.error(Errors.INVALID_REDIRECT_URI);
            throw new ErrorPageException(session, realm, uriInfo, headers, Messages.INVALID_PARAMETER, OIDCLoginProtocol.REDIRECT_URI_PARAM);
        }
    }

    private void createClientSession() {
        clientSession = session.sessions().createClientSession(realm, client);
        clientSession.setAuthMethod(OIDCLoginProtocol.LOGIN_PROTOCOL);
        clientSession.setRedirectUri(redirectUri);
        clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE);
        clientSession.setNote(ClientSessionCode.ACTION_KEY, KeycloakModelUtils.generateCodeSecret());
        clientSession.setNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, responseType);
        clientSession.setNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUriParam);

        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        if (scope != null) clientSession.setNote(OIDCLoginProtocol.SCOPE_PARAM, scope);
        if (loginHint != null) clientSession.setNote(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        if (prompt != null) clientSession.setNote(OIDCLoginProtocol.PROMPT_PARAM, prompt);
        if (idpHint != null) clientSession.setNote(AdapterConstants.KC_IDP_HINT, idpHint);
    }

    private Response buildAuthorizationCodeAuthorizationResponse() {
        String accessCode = new ClientSessionCode(realm, clientSession).getCode();

        if (idpHint != null && !"".equals(idpHint)) {
            IdentityProviderModel identityProviderModel = realm.getIdentityProviderByAlias(idpHint);

            if (identityProviderModel == null) {
                return Flows.forms(session, realm, null, uriInfo, headers)
                        .setError(Messages.IDENTITY_PROVIDER_NOT_FOUND, idpHint)
                        .createErrorPage();
            }
            return buildRedirectToIdentityProvider(idpHint, accessCode);
        }

        Response response = authManager.checkNonFormAuthentication(session, clientSession, realm, uriInfo, request, clientConnection, headers, event);
        if (response != null) return response;

        // SPNEGO/Kerberos authentication TODO: This should be somehow pluggable instead of hardcoded this way (Authentication interceptors?)
        HttpAuthenticationManager httpAuthManager = new HttpAuthenticationManager(session, clientSession, realm, uriInfo, request, clientConnection, event);
        HttpAuthenticationManager.HttpAuthOutput httpAuthOutput = httpAuthManager.spnegoAuthenticate(headers);
        if (httpAuthOutput.getResponse() != null) return httpAuthOutput.getResponse();

        if (prompt != null && prompt.equals("none")) {
            OIDCLoginProtocol oauth = new OIDCLoginProtocol(session, realm, uriInfo, headers, event);
            return oauth.cancelLogin(clientSession);
        }

        List<IdentityProviderModel> identityProviders = realm.getIdentityProviders();
        for (IdentityProviderModel identityProvider : identityProviders) {
            if (identityProvider.isAuthenticateByDefault()) {
                return buildRedirectToIdentityProvider(identityProvider.getAlias(), accessCode);
            }
        }

        List<RequiredCredentialModel> requiredCredentials = realm.getRequiredCredentials();
        if (requiredCredentials.isEmpty()) {
            if (!identityProviders.isEmpty()) {
                if (identityProviders.size() == 1) {
                    return buildRedirectToIdentityProvider(identityProviders.get(0).getAlias(), accessCode);
                }

                return Flows.forms(session, realm, null, uriInfo, headers).setError(Messages.IDENTITY_PROVIDER_NOT_UNIQUE, realm.getName()).createErrorPage();
            }

            return Flows.forms(session, realm, null, uriInfo, headers).setError(Messages.REALM_SUPPORTS_NO_CREDENTIALS, realm.getName()).createErrorPage();
        }

        LoginFormsProvider forms = Flows.forms(session, realm, clientSession.getClient(), uriInfo, headers)
                .setClientSessionCode(accessCode);

        // Attach state from SPNEGO authentication
        if (httpAuthOutput.getChallenge() != null) {
            httpAuthOutput.getChallenge().sendChallenge(forms);
        }

        String rememberMeUsername = AuthenticationManager.getRememberMeUsername(realm, headers);

        if (loginHint != null || rememberMeUsername != null) {
            MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();

            if (loginHint != null) {
                formData.add(AuthenticationManager.FORM_USERNAME, loginHint);
            } else {
                formData.add(AuthenticationManager.FORM_USERNAME, rememberMeUsername);
                formData.add("rememberMe", "on");
            }

            forms.setFormData(formData);
        }

        return forms.createLogin();
    }

    private Response buildRegister() {
        authManager.expireIdentityCookie(realm, uriInfo, clientConnection);

        return Flows.forms(session, realm, client, uriInfo, headers)
                .setClientSessionCode(new ClientSessionCode(realm, clientSession).getCode())
                .createRegistration();
    }

    private Response buildRedirectToIdentityProvider(String providerId, String accessCode) {
        logger.debug("Automatically redirect to identity provider: " + providerId);
        return Response.temporaryRedirect(
                Urls.identityProviderAuthnRequest(this.uriInfo.getBaseUri(), providerId, this.realm.getName(), accessCode))
                .build();
    }

}