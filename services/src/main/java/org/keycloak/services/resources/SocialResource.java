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

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.RequestDetails;
import org.keycloak.social.RequestDetailsBuilder;
import org.keycloak.social.SocialProvider;
import org.keycloak.social.SocialProviderConfig;
import org.keycloak.social.SocialProviderException;
import org.keycloak.social.SocialRequestManager;
import org.keycloak.social.SocialUser;

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
        return new Transaction() {
            protected Response callImpl() {
                Map<String, String[]> queryParams = getQueryParams();

                RequestDetails requestData = getRequestDetails(queryParams);
                SocialProvider provider = getProvider(requestData.getProviderId());

                String realmId = requestData.getClientAttribute("realmId");

                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.getRealm(realmId);

                OAuthFlows oauth = Flows.oauth(realm, request, uriInfo, authManager, tokenManager);

                if (!realm.isEnabled()) {
                    return oauth.forwardToSecurityFailure("Realm not enabled.");
                }

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

                String key = System.getProperty("keycloak.social." + requestData.getProviderId() + ".key");
                String secret = System.getProperty("keycloak.social." + requestData.getProviderId() + ".secret");
                String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();
                SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

                AuthCallback callback = new AuthCallback(requestData.getSocialAttributes(), queryParams);

                SocialUser socialUser = null;
                try {
                    socialUser = provider.processCallback(config, callback);
                } catch (SocialProviderException e) {
                    logger.warn("Failed to process social callback", e);
                    return oauth.forwardToSecurityFailure("Failed to process social callback");
                }

                // TODO Lookup user based on attribute for provider id - this is so a user can have a friendly username + link a
                // user to
                // multiple social logins
                UserModel user = realm.getUser(provider.getId() + "." + socialUser.getId());

                if (user == null) {
                    user = realm.addUser(provider.getId() + "." + socialUser.getId());
                    user.setAttribute(provider.getId() + ".id", socialUser.getId());

                    // TODO Grant default roles for realm when available
                    RoleModel defaultRole = realm.getRole("user");

                    realm.grantRole(user, defaultRole);
                }

                if (!user.isEnabled()) {
                    return oauth.forwardToSecurityFailure("Your account is not enabled.");
                }

                String scope = requestData.getClientAttributes().get("scope");
                String state = requestData.getClientAttributes().get("state");
                String redirectUri = requestData.getClientAttributes().get("redirectUri");

                return oauth.processAccessCode(scope, state, redirectUri, client, user);
            }
        }.call();
    }

    @GET
    @Path("{realm}/login")
    public Response redirectToProviderAuth(@PathParam("realm") final String realmId,
            @QueryParam("provider_id") final String providerId, @QueryParam("client_id") final String clientId,
            @QueryParam("scope") final String scope, @QueryParam("state") final String state,
            @QueryParam("redirect_uri") final String redirectUri) {
        SocialProvider provider = getProvider(providerId);
        if (provider == null) {
            return Flows.pages(request).forwardToSecurityFailure("Social provider not found");
        }

        String key = System.getProperty("keycloak.social." + providerId + ".key");
        String secret = System.getProperty("keycloak.social." + providerId + ".secret");
        String callbackUri = Urls.socialCallback(uriInfo.getBaseUri()).toString();

        SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

        try {
            AuthRequest authRequest = provider.getAuthUrl(config);

            RequestDetails socialRequest = RequestDetailsBuilder.create(providerId)
                    .putSocialAttributes(authRequest.getAttributes()).putClientAttribute("realmId", realmId)
                    .putClientAttribute("clientId", clientId).putClientAttribute("scope", scope)
                    .putClientAttribute("state", state).putClientAttribute("redirectUri", redirectUri).build();

            socialRequestManager.addRequest(authRequest.getId(), socialRequest);

            return Response.status(Status.FOUND).location(authRequest.getAuthUri()).build();
        } catch (Throwable t) {
            return Flows.pages(request).forwardToSecurityFailure("Failed to redirect to social auth");
        }
    }

    private RequestDetails getRequestDetails(Map<String, String[]> queryParams) {
        Iterator<SocialProvider> itr = ServiceRegistry.lookupProviders(SocialProvider.class);

        while (itr.hasNext()) {
            SocialProvider provider = itr.next();

            if (queryParams.containsKey(provider.getRequestIdParamName())) {
                String requestId = queryParams.get(provider.getRequestIdParamName())[0];
                if (socialRequestManager.isRequestId(requestId)) {
                    return socialRequestManager.retrieveData(requestId);
                }
            }
        }

        return null;
    }

    private SocialProvider getProvider(String providerId) {
        Iterator<SocialProvider> itr = ServiceRegistry.lookupProviders(SocialProvider.class);

        while (itr.hasNext()) {
            SocialProvider provider = itr.next();
            if (provider.getId().equals(providerId)) {
                return provider;
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
