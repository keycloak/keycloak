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
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SocialLinkModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.SocialRequestManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.RequestDetails;
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
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
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

        ClientModel client = realm.findClient(clientId);
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
        } catch (SocialAccessDeniedException e) {
            MultivaluedHashMap<String, String> queryParms = new MultivaluedHashMap<String, String>();
            queryParms.putSingle("client_id", requestData.getClientAttribute("clientId"));
            queryParms.putSingle("state", requestData.getClientAttribute("state"));
            queryParms.putSingle("scope", requestData.getClientAttribute("scope"));
            queryParms.putSingle("redirect_uri", requestData.getClientAttribute("redirectUri"));
            queryParms.putSingle("response_type", requestData.getClientAttribute("responseType"));
            return  Flows.forms(realm, request, uriInfo).setQueryParams(queryParms).setWarning("Access denied").createLogin();
        } catch (SocialProviderException e) {
            logger.warn("Failed to process social callback", e);
            return oauth.forwardToSecurityFailure("Failed to process social callback");
        }

        SocialLinkModel socialLink = new SocialLinkModel(provider.getId(), socialUser.getId(), socialUser.getUsername());
        UserModel user = realm.getUserBySocialLink(socialLink);

        // Check if user is already authenticated (this means linking social into existing user account)
        String userId = requestData.getClientAttribute("userId");
        if (userId != null) {
            UserModel authenticatedUser = realm.getUserById(userId);

            if (user != null) {
                return oauth.forwardToSecurityFailure("This social account is already linked to other user");
            }

            if (!authenticatedUser.isEnabled()) {
                return oauth.forwardToSecurityFailure("User is disabled");
            }
            if (!realm.hasRole(authenticatedUser, realm.getApplicationByName(Constants.ACCOUNT_MANAGEMENT_APP).getRole(AccountRoles.MANAGE_ACCOUNT))) {
                return oauth.forwardToSecurityFailure("Insufficient permissions to link social account");
            }

            realm.addSocialLink(authenticatedUser, socialLink);
            logger.debug("Social provider " + provider.getId() + " linked with user " + authenticatedUser.getLoginName());

            String redirectUri = requestData.getClientAttributes().get("redirectUri");
            if (redirectUri == null) {
                return oauth.forwardToSecurityFailure("Unknown redirectUri");
            }

            return Response.status(Status.FOUND).location(UriBuilder.fromUri(redirectUri).build()).build();
        }

        if (user == null) {
            if (!realm.isRegistrationAllowed()) {
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
                                           @QueryParam("redirect_uri") String redirectUri, @QueryParam("response_type") String responseType) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);

        SocialProvider provider = SocialLoader.load(providerId);
        if (provider == null) {
            return Flows.forms(realm, request, uriInfo).setError("Social provider not found").createErrorPage();
        }

        ClientModel client = realm.findClient(clientId);
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
            return Flows.social(socialRequestManager, realm, uriInfo, provider)
                    .putClientAttribute("realm", realmName)
                    .putClientAttribute("clientId", clientId).putClientAttribute("scope", scope)
                    .putClientAttribute("state", state).putClientAttribute("redirectUri", redirectUri)
                    .putClientAttribute("responseType", responseType).redirectToSocialProvider();
        } catch (Throwable t) {
            return Flows.forms(realm, request, uriInfo).setError("Failed to redirect to social auth").createErrorPage();
        }
    }

    private RequestDetails getRequestDetails(Map<String, String[]> queryParams) {
        String requestId = null;
        if (queryParams.containsKey("state")) {
            requestId =  queryParams.get("state")[0];
        } else if (queryParams.containsKey("oauth_token")) {
            requestId = queryParams.get("oauth_token")[0];
        } else if (queryParams.containsKey("denied")) {
            requestId = queryParams.get("denied")[0];
        }

        if (requestId != null && socialRequestManager.isRequestId(requestId)) {
            return socialRequestManager.retrieveData(requestId);
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
