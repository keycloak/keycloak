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

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.RequestDetails;
import org.keycloak.social.SocialLoader;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.social.SocialUser;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
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
    private HttpResponse response;

    @Context
    ResourceContext resourceContext;

    @Context
    protected KeycloakSession session;


    private SocialRequestManager socialRequestManager;

    private TokenManager tokenManager;

    private AuthenticationManager authManager = new AuthenticationManager();

    public SocialResource(TokenManager tokenManager, SocialRequestManager socialRequestManager) {
        this.tokenManager = tokenManager;
        this.socialRequestManager = socialRequestManager;
    }

    @GET
    @Path("callback")
    public Response callback() throws URISyntaxException {
        Map<String, String[]> queryParams = getQueryParams();

        RequestDetails requestData = getRequestDetails(queryParams);
        SocialProvider provider = SocialLoader.load(requestData.getProviderId());

        String realmName = requestData.getClientAttribute("realm");

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

        if (!realm.isEnabled()) {
            return oauth.forwardToSecurityFailure("Realm not enabled.");
        }

        String clientId = requestData.getClientAttributes().get("clientId");

        UserModel client = realm.getUser(clientId);
        if (client == null) {
            return oauth.forwardToSecurityFailure("Unknown login requester.");
        }
        if (!client.isEnabled()) {
            return oauth.forwardToSecurityFailure("Login requester not enabled.");
        }

        String key = realm.getSocialConfig().get(requestData.getProviderId() + ".key");
        String secret = realm.getSocialConfig().get(requestData.getProviderId() + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();
        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

        AuthCallback callback = new AuthCallback(requestData.getSocialAttributes(), queryParams);

        SocialUser socialUser;
        try {
            socialUser = provider.processCallback(config, callback);
        } catch (SocialProviderException e) {
            logger.warn("Failed to process social callback", e);
            return oauth.forwardToSecurityFailure("Failed to process social callback");
        }

        SocialLinkModel socialLink = new SocialLinkModel(provider.getId(), socialUser.getUsername());
        UserModel user = realm.getUserBySocialLink(socialLink);

        if (user == null) {
            if (!realm.isRegistrationAllowed()) {
                return oauth.forwardToSecurityFailure("Registration not allowed");
            }

            // Automatically register user into realm with his social username (don't redirect to registration screen)
            if (realm.getUser(socialUser.getUsername()) != null) {
                // TODO: Username is already in realm. Show message and let user to bind accounts after he re-authenticate
                throw new IllegalStateException("Username " + socialUser.getUsername() +
                        " already registered in the realm. TODO: bind accounts...");

                // TODO: Maybe we should search also by email and bind accounts if user with this email is
                // already registered. But actually Keycloak allows duplicate emails
            } else {
                user = realm.addUser(socialUser.getUsername());
                user.setEnabled(true);
                user.setFirstName(socialUser.getFirstName());
                user.setLastName(socialUser.getLastName());
                user.setEmail(socialUser.getEmail());

                if (realm.isUpdateProfileOnInitialSocialLogin()) {
                    user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
                }
            }

            realm.addSocialLink(user, socialLink);
        }

        if (!user.isEnabled()) {
            return oauth.forwardToSecurityFailure("Your account is not enabled.");
        }

        String scope = requestData.getClientAttributes().get("scope");
        String state = requestData.getClientAttributes().get("state");
        String redirectUri = requestData.getClientAttributes().get("redirectUri");

        return oauth.processAccessCode(scope, state, redirectUri, client, user);
    }

    @GET
    @Path("{realm}/login")
    public Response redirectToProviderAuth(@PathParam("realm") final String realmName,
                                           @QueryParam("provider_id") final String providerId, @QueryParam("client_id") final String clientId,
                                           @QueryParam("scope") final String scope, @QueryParam("state") final String state,
                                           @QueryParam("redirect_uri") String redirectUri) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        SocialProvider provider = SocialLoader.load(providerId);
        if (provider == null) {
            return Flows.forms(realm, request, uriInfo).setError("Social provider not found").createErrorPage();
        }

        String key = realm.getSocialConfig().get(providerId + ".key");
        String secret = realm.getSocialConfig().get(providerId + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();

        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

        UserModel client = realm.getUser(clientId);
        if (client == null) {
            logger.warn("Unknown login requester: " + clientId);
            return Flows.forms(realm, request, uriInfo).setError("Unknown login requester.").createErrorPage();
        }

        if (!client.isEnabled()) {
            logger.warn("Login requester not enabled.");
            return Flows.forms(realm, request, uriInfo).setError("Login requester not enabled.").createErrorPage();
        }
        redirectUri = TokenService.verifyRedirectUri(redirectUri, client);
        if (redirectUri == null) {
            return Flows.forms(realm, request, uriInfo).setError("Invalid redirect_uri.").createErrorPage();
        }

        try {
            AuthRequest authRequest = provider.getAuthUrl(config);

            RequestDetails socialRequest = RequestDetails.create(providerId)
                    .putSocialAttributes(authRequest.getAttributes()).putClientAttribute("realm", realmName)
                    .putClientAttribute("clientId", clientId).putClientAttribute("scope", scope)
                    .putClientAttribute("state", state).putClientAttribute("redirectUri", redirectUri).build();

            socialRequestManager.addRequest(authRequest.getId(), socialRequest);

            return Response.status(Status.FOUND).location(authRequest.getAuthUri()).build();
        } catch (Throwable t) {
            return Flows.forms(realm, request, uriInfo).setError("Failed to redirect to social auth").createErrorPage();
        }
    }

    private RequestDetails getRequestDetails(Map<String, String[]> queryParams) {
        for (SocialProvider provider : SocialLoader.load()) {
            if (queryParams.containsKey(provider.getRequestIdParamName())) {
                String requestId = queryParams.get(provider.getRequestIdParamName())[0];
                if (socialRequestManager.isRequestId(requestId)) {
                    return socialRequestManager.retrieveData(requestId);
                }
            }
        }

        return null;
    }

    private Map<String, String[]> getQueryParams() {
        Map<String, String[]> queryParams = new HashMap<String, String[]>();
        for (Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
            queryParams.put(e.getKey(), e.getValue().toArray(new String[e.getValue().size()]));
        }
        return queryParams;
    }

}
