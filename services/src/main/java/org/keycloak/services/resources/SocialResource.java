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
import org.keycloak.ClientConnection;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.EventsManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.flows.Flows;
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

    @GET
    @Path("callback")
    public Response callback(@QueryParam("state") String encodedState) throws URISyntaxException, IOException {
        ClientSessionCode clientCode = null;
        ClientSessionModel clientSession = null;
        try {
            clientCode = ClientSessionCode.parse(encodedState, session);
            if (clientCode == null) {
                return Flows.forms(session, null, null, uriInfo).setError("Unexpected callback").createErrorPage();
            }
            clientSession = clientCode.getClientSession();
            if (!clientCode.isValid(ClientSessionModel.Action.SOCIAL_CALLBACK)) {
                return Flows.forwardToSecurityFailurePage(session, clientSession.getRealm(), uriInfo, "Invalid code, please login again through your application.");
            }
        } catch (Throwable t) {
            logger.error("Invalid social callback", t);
            return Flows.forms(session, null, null, uriInfo).setError("Unexpected callback").createErrorPage();
        }
        String providerId = clientSession.getNote("social_provider");
        SocialProvider provider = SocialLoader.load(providerId);

        String authMethod = "social@" + provider.getId();

        RealmModel realm = clientSession.getRealm();

        EventBuilder event = new EventsManager(realm, session, clientConnection).createEventBuilder()
                .event(EventType.LOGIN)
                .client(clientSession.getClient())
                .detail(Details.REDIRECT_URI, clientSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, authMethod);

        if (!realm.isEnabled()) {
            event.error(Errors.REALM_DISABLED);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Realm not enabled.");
        }

        String key = realm.getSocialConfig().get(provider.getId() + ".key");
        String secret = realm.getSocialConfig().get(provider.getId() + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();
        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

        Map<String, String[]> queryParams = getQueryParams();

        AuthCallback callback = new AuthCallback(queryParams);

        SocialUser socialUser;
        try {
            socialUser = provider.processCallback(clientSession, config, callback);
        } catch (SocialAccessDeniedException e) {
            event.error(Errors.REJECTED_BY_USER);
            clientSession.setAction(ClientSessionModel.Action.AUTHENTICATE);
            return  Flows.forms(session, realm, clientSession.getClient(), uriInfo).setClientSessionCode(clientCode.getCode()).setWarning("Access denied").createLogin();
        } catch (SocialProviderException e) {
            logger.error("Failed to process social callback", e);
            return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Failed to process social callback");
        }

        event.detail(Details.USERNAME, socialUser.getId() + "@" + provider.getId());

        try {
            SocialLinkModel socialLink = new SocialLinkModel(provider.getId(), socialUser.getId(), socialUser.getUsername());
            UserModel user = session.users().getUserBySocialLink(socialLink, realm);

            // Check if user is already authenticated (this means linking social into existing user account)
            if (clientSession.getUserSession() != null) {

                UserModel authenticatedUser = clientSession.getUserSession().getUser();

                event.event(EventType.SOCIAL_LINK).user(authenticatedUser.getId());

                if (user != null) {
                    event.error(Errors.SOCIAL_ID_IN_USE);
                    return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "This social account is already linked to other user");
                }

                if (!authenticatedUser.isEnabled()) {
                    event.error(Errors.USER_DISABLED);
                    return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "User is disabled");
                }

                if (!authenticatedUser.hasRole(realm.getApplicationByName(Constants.ACCOUNT_MANAGEMENT_APP).getRole(AccountRoles.MANAGE_ACCOUNT))) {
                    event.error(Errors.NOT_ALLOWED);
                    return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Insufficient permissions to link social account");
                }

                session.users().addSocialLink(realm, authenticatedUser, socialLink);
                logger.debugv("Social provider {0} linked with user {1}", provider.getId(), authenticatedUser.getUsername());

                event.success();
                return Response.status(302).location(UriBuilder.fromUri(clientSession.getRedirectUri()).build()).build();
            }

            if (user == null) {
                user = session.users().addUser(realm, KeycloakModelUtils.generateId());
                user.setEnabled(true);
                user.setFirstName(socialUser.getFirstName());
                user.setLastName(socialUser.getLastName());
                user.setEmail(socialUser.getEmail());

                if (realm.isUpdateProfileOnInitialSocialLogin()) {
                    user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
                }

                session.users().addSocialLink(realm, user, socialLink);

                event.clone().user(user).event(EventType.REGISTER)
                        .detail(Details.REGISTER_METHOD, "social@" + provider.getId())
                        .detail(Details.EMAIL, socialUser.getEmail())
                        .removeDetail("auth_method")
                        .success();
            }

            event.user(user);

            if (!user.isEnabled()) {
                event.error(Errors.USER_DISABLED);
                return Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Your account is not enabled.");
            }

            String username = socialLink.getSocialUserId() + "@" + socialLink.getSocialProvider();

            UserSessionModel userSession = session.sessions().createUserSession(realm, user, username, clientConnection.getRemoteAddr(), authMethod, false);
            event.session(userSession);
            TokenManager.attachClientSession(userSession, clientSession);

            AuthenticationManager authManager = new AuthenticationManager();
            Response response = authManager.nextActionAfterAuthentication(session, userSession, clientSession, clientConnection, request, uriInfo, event);
            if (session.getTransaction().isActive()) {
                session.getTransaction().commit();
            }
            return response;
        } catch (ModelDuplicateException e) {
            // Assume email is the duplicate as there's nothing else atm
            return Flows.forms(session, realm, clientSession.getClient(), uriInfo)
                    .setClientSessionCode(clientCode.getCode())
                    .setError("socialEmailExists")
                    .createLogin();
        }
    }

    private class Checks {
        ClientSessionCode clientCode;
        Response response;

        private boolean checkSsl(RealmModel realm) {
            if (uriInfo.getBaseUri().getScheme().equals("https")) {
                return true;
            } else {
                return !realm.getSslRequired().isRequired(clientConnection);
            }
        }


        boolean check(EventBuilder event, RealmModel realm, String code, ClientSessionModel.Action requiredAction) {
            if (!checkSsl(realm)) {
                event.error(Errors.SSL_REQUIRED);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "HTTPS required");
                return false;
            }
            if (!realm.isEnabled()) {
                event.error(Errors.REALM_DISABLED);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Realm not enabled.");
                return false;
            }
            clientCode = ClientSessionCode.parse(code, session, realm);
            if (clientCode == null) {
                event.error(Errors.INVALID_CODE);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Unknown code, please login again through your application.");
                return false;
            }
            if (!clientCode.isValid(requiredAction)) {
                event.error(Errors.INVALID_CODE);
                response = Flows.forwardToSecurityFailurePage(session, realm, uriInfo, "Invalid code, please login again through your application.");
            }
            return true;
        }
    }


    @GET
    @Path("{realm}/login")
    public Response redirectToProviderAuth(@PathParam("realm") final String realmName,
                                           @QueryParam("provider_id") final String providerId,
                                           @QueryParam("code") String code) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        EventBuilder event = new EventsManager(realm, session, clientConnection).createEventBuilder()
                .event(EventType.LOGIN)
                .detail(Details.AUTH_METHOD, "social@" + providerId);

        SocialProvider provider = SocialLoader.load(providerId);
        if (provider == null) {
            event.error(Errors.SOCIAL_PROVIDER_NOT_FOUND);
            return Flows.forms(session, realm, null, uriInfo).setError("Social provider not found").createErrorPage();
        }

        Checks checks = new Checks();
        if (!checks.check(event, realm, code, ClientSessionModel.Action.AUTHENTICATE)) {
            return checks.response;
        }

        ClientSessionCode clientSessionCode = checks.clientCode;

        try {
            return Flows.social(realm, uriInfo, clientConnection, provider)
                    .redirectToSocialProvider(clientSessionCode);
        } catch (Throwable t) {
            logger.error("Failed to redirect to social auth", t);
            return Flows.forms(session, realm, null, uriInfo).setError("Failed to redirect to social auth").createErrorPage();
        }
    }

    private Response returnToLogin(RealmModel realm, ClientModel client, Map<String, String> attributes, String error) {
        MultivaluedMap<String, String> q = new MultivaluedMapImpl<String, String>();
        for (Entry<String, String> e : attributes.entrySet()) {
            q.add(e.getKey(), e.getValue());
        }
        return Flows.forms(session, realm, client, uriInfo)
                .setQueryParams(q)
                .setError(error)
                .createLogin();
    }

    private Map<String, String[]> getQueryParams() {
        Map<String, String[]> queryParams = new HashMap<String, String[]>();
        for (Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
            queryParams.put(e.getKey(), e.getValue().toArray(new String[e.getValue().size()]));
        }
        return queryParams;
    }

}
