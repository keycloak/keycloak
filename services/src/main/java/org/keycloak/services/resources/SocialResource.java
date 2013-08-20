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
import java.util.UUID;

import javax.imageio.spi.ServiceRegistry;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RoleModel;
import org.keycloak.services.models.SocialLinkModel;
import org.keycloak.services.models.UserModel;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.services.resources.flows.OAuthFlows;
import org.keycloak.services.resources.flows.PageFlows;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.social.AuthCallback;
import org.keycloak.social.AuthRequest;
import org.keycloak.social.RequestDetails;
import org.keycloak.social.RequestDetailsBuilder;
import org.keycloak.social.SocialConstants;
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

    @Context
    private HttpResponse response;

    @Context
    ResourceContext resourceContext;

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
        return new Transaction<Response>() {
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

                SocialLinkModel socialLink = new SocialLinkModel(provider.getId(), socialUser.getUsername());
                UserModel user = realm.getUserBySocialLink(socialLink);

                if (user == null) {
                    if (!realm.isRegistrationAllowed()) {
                        return oauth.forwardToSecurityFailure("Registration not allowed");
                    }

                    // Automatically register user into realm with his social username (don't redirect to registration screen)
                    if (realm.isAutomaticRegistrationAfterSocialLogin()) {

                        if (realm.getUser(socialUser.getUsername()) != null) {
                            // TODO: Username is already in realm. Show message and let user to bind accounts after he re-authenticate
                            throw new IllegalStateException("Username " + socialUser.getUsername() +
                                    " already registered in the realm. TODO: bind accounts...");

                            // TODO: Maybe we should search also by email and bind accounts if user with this email is
                            // already registered. But actually Keycloak allows duplicate emails
                        } else {
                            user = realm.addUser(socialUser.getUsername());
                            user.setFirstName(socialUser.getFirstName());
                            user.setLastName(socialUser.getLastName());
                            user.setEmail(socialUser.getEmail());
                        }

                        realm.addSocialLink(user, socialLink);

                        for (RoleModel role : realm.getDefaultRoles()) {
                            realm.grantRole(user, role);
                        }
                    }  else {
                        // Redirect user to registration screen with prefilled data from social provider
                        MultivaluedMap<String, String> formData = fillRegistrationFormWithSocialData(socialUser);

                        RequestDetailsBuilder reqDetailsBuilder = RequestDetailsBuilder.createFromRequestDetails(requestData);
                        reqDetailsBuilder.putSocialAttribute(SocialConstants.ATTR_SOCIAL_LINK, socialLink);

                        String requestId = UUID.randomUUID().toString();
                        socialRequestManager.addRequest(requestId, reqDetailsBuilder.build());
                        boolean secureOnly = !realm.isSslNotRequired();
                        String cookiePath = Urls.socialBase(uriInfo.getBaseUri()).build().getPath();
                        logger.info("creating cookie for social registration - name: " + SocialConstants.SOCIAL_REGISTRATION_COOKIE
                                + " path: " + cookiePath);
                        NewCookie newCookie = new NewCookie(SocialConstants.SOCIAL_REGISTRATION_COOKIE, requestId,
                                cookiePath, null, "Added social cookie", NewCookie.DEFAULT_MAX_AGE, secureOnly);
                        response.addNewCookie(newCookie);

                        return Flows.forms(realm, request).setFormData(formData).setSocialRegistration(true).forwardToRegistration();
                    }
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

    @POST
    @Path("{realm}/socialRegistration")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response socialRegistration(@PathParam("realm") final String realmId,
                                       final MultivaluedMap<String, String> formData) {
        return new Transaction<Response>() {
            protected Response callImpl() {
                PageFlows pageFlows = Flows.pages(request);
                Cookie cookie = headers.getCookies().get(SocialConstants.SOCIAL_REGISTRATION_COOKIE);
                if (cookie == null) {
                    return pageFlows.forwardToSecurityFailure("Social registration cookie not found");
                }

                String requestId = cookie.getValue();
                if (!socialRequestManager.isRequestId(requestId)) {
                    logger.error("Unknown requestId found in cookie. Maybe it's expired. requestId=" + requestId);
                    return pageFlows.forwardToSecurityFailure("Unknown requestId found in cookie. Maybe it's expired.");
                }

                RequestDetails requestData = socialRequestManager.getData(requestId);

                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.getRealm(realmId);
                if (realm == null || !realm.isEnabled()) {
                    return pageFlows.forwardToSecurityFailure("Realm doesn't exists or is not enabled.");
                }
                TokenService tokenService = new TokenService(realm, tokenManager);
                resourceContext.initResource(tokenService);

                String clientId = requestData.getClientAttribute("clientId");
                String scope = requestData.getClientAttribute("scope");
                String state = requestData.getClientAttribute("state");
                String redirectUri = requestData.getClientAttribute("redirectUri");
                SocialLinkModel socialLink = (SocialLinkModel)requestData.getSocialAttribute(SocialConstants.ATTR_SOCIAL_LINK);

                Response response1 = tokenService.processRegisterImpl(clientId, scope, state, redirectUri, formData, true);

                // Some error occured during registration
                if (response1 == null) {
                    return null;
                }

                String username = formData.getFirst("username");
                UserModel user = realm.getUser(username);
                if (user == null) {
                    // Normally shouldn't happen
                    throw new IllegalStateException("User " + username + " not found in the realm");
                }
                realm.addSocialLink(user, socialLink);

                // Expire cookie and invalidate requestData
                String cookiePath = Urls.socialBase(uriInfo.getBaseUri()).build().getPath();
                NewCookie newCookie = new NewCookie(SocialConstants.SOCIAL_REGISTRATION_COOKIE, "", cookiePath, null,
                        "Expire social cookie", 0, false);
                logger.info("Expiring social registration cookie: " + SocialConstants.SOCIAL_REGISTRATION_COOKIE + ", path: " + cookiePath);
                response.addNewCookie(newCookie);
                socialRequestManager.retrieveData(requestId);

                return response1;
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

    protected MultivaluedMap<String, String> fillRegistrationFormWithSocialData(SocialUser socialUser) {
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl<String, String>();
        formData.putSingle("username", socialUser.getUsername());

        if (socialUser.getEmail() != null) {
            formData.putSingle("email", socialUser.getEmail());
        }

        String fullName = null;
        if (socialUser.getFirstName() == null) {
            fullName = socialUser.getLastName();
        } else if (socialUser.getLastName() == null) {
            fullName = socialUser.getFirstName();
        } else {
            fullName = socialUser.getFirstName() + " " + socialUser.getLastName();
        }

        formData.putSingle("name", fullName);
        return formData;
    }

}
