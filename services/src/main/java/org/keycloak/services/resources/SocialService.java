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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.UserModel;
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
public class SocialService extends AbstractLoginService {

    private static final Logger logger = Logger.getLogger(SocialService.class);

    @Context
    private HttpHeaders headers;

    @Context
    private UriInfo uriInfo;

    public static UriBuilder socialServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder().path(RealmsResource.class).path(RealmsResource.class, "getSocialService");
        return base;
    }

    public static UriBuilder redirectToProviderAuthUrl(UriInfo uriInfo) {
        return socialServiceBaseUrl(uriInfo).path(SocialService.class, "redirectToProviderAuth");

    }

    public static UriBuilder callbackUrl(UriInfo uriInfo) {
        return socialServiceBaseUrl(uriInfo).path(SocialService.class, "callback");

    }

    private SocialRequestManager socialRequestManager;

    public SocialService(RealmModel realm, TokenManager tokenManager, SocialRequestManager socialRequestManager) {
        super(realm, tokenManager);
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

                String key = System.getProperty("keycloak.social." + requestData.getProviderId() + ".key");
                String secret = System.getProperty("keycloak.social." + requestData.getProviderId() + ".secret");
                String callbackUri = callbackUrl(uriInfo).build(realm.getId()).toString();

                SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

                AuthCallback callback = new AuthCallback(requestData.getSocialAttributes(), queryParams);

                SocialUser socialUser = null;
                try {
                    socialUser = provider.processCallback(config, callback);
                } catch (SocialProviderException e) {
                    logger.warn("Failed to process social callback", e);
                    securityFailureForward("Failed to process social callback");
                    return null;
                }

                if (!realm.isEnabled()) {
                    securityFailureForward("Realm not enabled.");
                    return null;
                }

                String clientId = requestData.getClientAttributes().get("clientId");

                UserModel client = realm.getUser(clientId);
                if (client == null) {
                    securityFailureForward("Unknown login requester.");
                    return null;
                }
                if (!client.isEnabled()) {
                    securityFailureForward("Login requester not enabled.");
                    return null;
                }

                // TODO Lookup user based on attribute for provider id - this is so a user can have a friendly username + link a
                // user to
                // multiple social logins
                UserModel user = realm.getUser(provider.getId() + "." + socialUser.getId());

                if (user == null) {
                    user = realm.addUser(provider.getId() + "." + socialUser.getId());
                    user.setAttribute(provider.getId() + ".id", socialUser.getId());

                    // TODO Grant default roles for realm when available
                    realm.grantRole(user, realm.getRole("user"));
                }

                if (!user.isEnabled()) {
                    securityFailureForward("Your account is not enabled.");
                    return null;
                }

                String scope = requestData.getClientAttributes().get("scope");
                String state = requestData.getClientAttributes().get("state");
                String redirectUri = requestData.getClientAttributes().get("redirectUri");

                return processAccessCode(scope, state, redirectUri, client, user);
            }
        }.call();
    }

    @GET
    @Path("providers")
    @Produces(MediaType.APPLICATION_JSON)
    public List<SocialProvider> getProviders() {
        List<SocialProvider> providers = new LinkedList<SocialProvider>();
        Iterator<SocialProvider> itr = ServiceRegistry.lookupProviders(SocialProvider.class);
        while (itr.hasNext()) {
            providers.add(itr.next());
        }
        return providers;
    }

    @GET
    @Path("login")
    public Response redirectToProviderAuth(@QueryParam("provider_id") final String providerId,
            @QueryParam("client_id") final String clientId, @QueryParam("scope") final String scope,
            @QueryParam("state") final String state, @QueryParam("redirect_uri") final String redirectUri) {
        return new Transaction() {
            protected Response callImpl() {
                SocialProvider provider = getProvider(providerId);
                if (provider == null) {
                    securityFailureForward("Social provider not found");
                    return null;
                }

                String key = System.getProperty("keycloak.social." + providerId + ".key");
                String secret = System.getProperty("keycloak.social." + providerId + ".secret");
                String callbackUri = callbackUrl(uriInfo).build(realm.getId()).toString();

                SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

                try {
                    AuthRequest authRequest = provider.getAuthUrl(config);

                    RequestDetails socialRequest = RequestDetailsBuilder.create(providerId)
                            .putSocialAttributes(authRequest.getAttributes()).putClientAttribute("clientId", clientId)
                            .putClientAttribute("scope", scope).putClientAttribute("state", state)
                            .putClientAttribute("redirectUri", redirectUri).build();

                    socialRequestManager.addRequest(authRequest.getId(), socialRequest);

                    return Response.status(Status.FOUND).location(authRequest.getAuthUri()).build();
                } catch (Throwable t) {
                    logger.error("Failed to redirect to social auth", t);
                    securityFailureForward("Failed to redirect to social auth");
                    return null;
                }
            }
        }.call();
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

    @Override
    protected Logger getLogger() {
        return logger;
    }

}
