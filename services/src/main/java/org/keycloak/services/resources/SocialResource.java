/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.resources;

import org.jboss.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.audit.Audit;
import org.keycloak.audit.Details;
import org.keycloak.audit.Errors;
import org.keycloak.audit.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.ClientConnection;
import org.keycloak.services.managers.AuditManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.SocialAccessDeniedException;
import org.keycloak.social.SocialLoader;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialUser;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@Path("/social")
public class SocialResource {

    protected static Logger logger = Logger.getLogger(SocialResource.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpHeaders headers;

    @Context
    private HttpRequest request;

    @Context
    protected KeycloakSession session;

    @Context
    protected ClientConnection clientConnection;

    private TokenManager tokenManager;

    public SocialResource(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @GET
    @Path("callback")
    public Response callback(@QueryParam("state") String encodedState) throws URISyntaxException, IOException {
        State initialRequest;
        try {
            initialRequest = new JWSInput(encodedState).readJsonContent(State.class);
        } catch (Throwable t) {
            logger.warn("Invalid social callback", t);
            return Flows.forms(session, null, uriInfo).setError("Unexpected callback").createErrorPage();
        }

        SocialProvider provider = SocialLoader.load(initialRequest.getProvider());

        String realmName = initialRequest.getRealm();

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        Audit audit = new AuditManager(realm, session, clientConnection).createAudit()
                .event(EventType.LOGIN)
                .detail(Details.RESPONSE_TYPE, initialRequest.get(OAuth2Constants.RESPONSE_TYPE))
                .detail(Details.AUTH_METHOD, "social@" + provider.getId());

        AuthenticationManager authManager = new AuthenticationManager(session);
        OAuthFlows oauth = Flows.oauth(session, realm, request, uriInfo, authManager, tokenManager);

        if (!realm.isEnabled()) {
            audit.error(Errors.REALM_DISABLED);
            return oauth.forwardToSecurityFailure("Realm not enabled.");
        }

        String clientId = initialRequest.get(OAuth2Constants.CLIENT_ID);
        String redirectUri = initialRequest.get(OAuth2Constants.REDIRECT_URI);
        String scope = initialRequest.get(OAuth2Constants.SCOPE);
        String state = initialRequest.get(OAuth2Constants.STATE);
        String responseType = initialRequest.get(OAuth2Constants.RESPONSE_TYPE);

        audit.client(clientId).detail(Details.REDIRECT_URI, redirectUri);

        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            audit.error(Errors.CLIENT_NOT_FOUND);
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }
        if (!client.isEnabled()) {
            audit.error(Errors.CLIENT_DISABLED);
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        String key = realm.getSocialConfig().get(provider.getId() + ".key");
        String secret = realm.getSocialConfig().get(provider.getId() + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();
        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

        Map<String, String[]> queryParams = getQueryParams();
        Map<String, String> attributes = getAttributes();

        AuthCallback callback = new AuthCallback(queryParams, attributes);

        SocialUser socialUser;
        try {
            socialUser = provider.processCallback(config, callback);
        } catch (SocialAccessDeniedException e) {
            MultivaluedMap<String, String> queryParms = new MultivaluedMapImpl<String, String>();
            queryParms.putSingle(OAuth2Constants.CLIENT_ID, clientId);
            queryParms.putSingle(OAuth2Constants.STATE, state);
            queryParms.putSingle(OAuth2Constants.SCOPE, scope);
            queryParms.putSingle(OAuth2Constants.REDIRECT_URI, redirectUri);
            queryParms.putSingle(OAuth2Constants.RESPONSE_TYPE, responseType);

            audit.error(Errors.REJECTED_BY_USER);
            return  Flows.forms(session, realm, uriInfo).setQueryParams(queryParms).setWarning("Access denied").createLogin();
        } catch (SocialProviderException e) {
            logger.error("Failed to process social callback", e);
            return oauth.forwardToSecurityFailure("Failed to process social callback");
        }

        audit.detail(Details.USERNAME, socialUser.getId() + "@" + provider.getId());

        SocialLinkModel socialLink = new SocialLinkModel(provider.getId(), socialUser.getId(), socialUser.getUsername());
        UserModel user = realm.getUserBySocialLink(socialLink);

        // Check if user is already authenticated (this means linking social into existing user account)
        String userId = initialRequest.getUser();
        if (userId != null) {
            UserModel authenticatedUser = realm.getUserById(userId);

            audit.event(EventType.SOCIAL_LINK).user(userId);

            if (user != null) {
                audit.error(Errors.SOCIAL_ID_IN_USE);
                return oauth.forwardToSecurityFailure("This social account is already linked to other user");
            }

            if (!authenticatedUser.isEnabled()) {
                audit.error(Errors.USER_DISABLED);
                return oauth.forwardToSecurityFailure("User is disabled");
            }

            if (!authenticatedUser.hasRole(realm.getApplicationByName(Constants.ACCOUNT_MANAGEMENT_APP).getRole(AccountRoles.MANAGE_ACCOUNT))) {
                audit.error(Errors.NOT_ALLOWED);
                return oauth.forwardToSecurityFailure("Insufficient permissions to link social account");
            }

            if (redirectUri == null) {
                audit.error(Errors.INVALID_REDIRECT_URI);
                return oauth.forwardToSecurityFailure("Unknown redirectUri");
            }

            realm.addSocialLink(authenticatedUser, socialLink);
            logger.debug("Social provider " + provider.getId() + " linked with user " + authenticatedUser.getLoginName());

            audit.success();
            return Response.status(302).location(UriBuilder.fromUri(redirectUri).build()).build();
        }

        if (user == null) {

            if (!realm.isRegistrationAllowed()) {
                audit.error(Errors.REGISTRATION_DISABLED);
                return oauth.forwardToSecurityFailure("Registration not allowed");
            }

            user = realm.addUser(KeycloakModelUtils.generateId());
            user.setEnabled(true);
            user.setFirstName(socialUser.getFirstName());
            user.setLastName(socialUser.getLastName());
            user.setEmail(socialUser.getEmail());

            if (realm.isUpdateProfileOnInitialSocialLogin()) {
                user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            }

            realm.addSocialLink(user, socialLink);

            audit.clone().user(user).event(EventType.REGISTER)
                    .detail(Details.REGISTER_METHOD, "social@" + provider.getId())
                    .detail(Details.EMAIL, socialUser.getEmail())
                    .removeDetail("auth_method")
                    .success();
        }

        audit.user(user);

        if (!user.isEnabled()) {
            audit.error(Errors.USER_DISABLED);
            return oauth.forwardToSecurityFailure("Your account is not enabled.");
        }

        UserSessionModel session = realm.createUserSession(user, clientConnection.getRemoteAddr());
        audit.session(session);

        return oauth.processAccessCode(scope, state, redirectUri, client, user, session, socialLink.getSocialUserId() + "@" + socialLink.getSocialProvider(), false, "social@" + provider.getId(), audit);
    }

    @GET
    @Path("{realm}/login")
    public Response redirectToProviderAuth(@PathParam("realm") final String realmName,
                                           @QueryParam("provider_id") final String providerId, @QueryParam(OAuth2Constants.CLIENT_ID) final String clientId,
                                           @QueryParam("scope") final String scope, @QueryParam("state") final String state,
                                           @QueryParam("redirect_uri") String redirectUri, @QueryParam("response_type") String responseType) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        Audit audit = new AuditManager(realm, session, clientConnection).createAudit()
                .event(EventType.LOGIN).client(clientId)
                .detail(Details.REDIRECT_URI, redirectUri)
                .detail(Details.RESPONSE_TYPE, "code")
                .detail(Details.AUTH_METHOD, "social@" + providerId);

        SocialProvider provider = SocialLoader.load(providerId);
        if (provider == null) {
            audit.error(Errors.SOCIAL_PROVIDER_NOT_FOUND);
            return Flows.forms(session, realm, uriInfo).setError("Social provider not found").createErrorPage();
        }

        ClientModel client = realm.findClient(clientId);
        if (client == null) {
            audit.error(Errors.CLIENT_NOT_FOUND);
            logger.warn("Unknown login requester: " + clientId);
            return Flows.forms(session, realm, uriInfo).setError("Unknown login requester.").createErrorPage();
        }

        if (!client.isEnabled()) {
            audit.error(Errors.CLIENT_DISABLED);
            logger.warn("Login requester not enabled.");
            return Flows.forms(session, realm, uriInfo).setError("Login requester not enabled.").createErrorPage();
        }
        redirectUri = TokenService.verifyRedirectUri(uriInfo, redirectUri, realm, client);
        if (redirectUri == null) {
            audit.error(Errors.INVALID_REDIRECT_URI);
            return Flows.forms(session, realm, uriInfo).setError("Invalid redirect_uri.").createErrorPage();
        }

        try {
            return Flows.social(realm, uriInfo, provider)
                    .putClientAttribute(OAuth2Constants.CLIENT_ID, clientId)
                    .putClientAttribute(OAuth2Constants.SCOPE, scope)
                    .putClientAttribute(OAuth2Constants.STATE, state)
                    .putClientAttribute(OAuth2Constants.REDIRECT_URI, redirectUri)
                    .putClientAttribute(OAuth2Constants.RESPONSE_TYPE, responseType)
                    .redirectToSocialProvider();
        } catch (Throwable t) {
            logger.error("Failed to redirect to social auth", t);
            return Flows.forms(session, realm, uriInfo).setError("Failed to redirect to social auth").createErrorPage();
        }
    }

    private Map<String, String[]> getQueryParams() {
        Map<String, String[]> queryParams = new HashMap<String, String[]>();
        for (Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
            queryParams.put(e.getKey(), e.getValue().toArray(new String[e.getValue().size()]));
        }
        return queryParams;
    }

    private Map<String, String> getAttributes() throws IOException {
        Cookie cookie = headers.getCookies().get("KEYCLOAK_SOCIAL");
        return cookie != null ? new JWSInput(cookie.getValue()).readJsonContent(HashMap.class) : null;
    }

    public static class State {
        private String realm;
        private String provider;
        private String user;
        private Map<String, String> attributes  = new HashMap<String, String>();

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public String get(String key) {
            return attributes.get(key);
        }

        public void set(String key, String value) {
            attributes.put(key, value);
        }
    }

}
