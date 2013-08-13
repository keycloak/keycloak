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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.resteasy.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
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

    public static UriBuilder socialServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder base = uriInfo.getBaseUriBuilder().path(SocialResource.class);
        return base;
    }

    public static UriBuilder redirectToProviderAuthUrl(UriInfo uriInfo) {
        return socialServiceBaseUrl(uriInfo).path(SocialResource.class, "redirectToProviderAuth");
    }

    public static UriBuilder callbackUrl(UriInfo uriInfo) {
        return socialServiceBaseUrl(uriInfo).path(SocialResource.class, "callback");
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

                String key = System.getProperty("keycloak.social." + requestData.getProviderId() + ".key");
                String secret = System.getProperty("keycloak.social." + requestData.getProviderId() + ".secret");
                String callbackUri = callbackUrl(uriInfo).build().toString();

                SocialProviderConfig config = new SocialProviderConfig(key, secret, callbackUri);

                AuthCallback callback = new AuthCallback(requestData.getSocialAttributes(), queryParams);

                SocialUser socialUser = null;
                try {
                    socialUser = provider.processCallback(config, callback);
                } catch (SocialProviderException e) {
                    logger.warn("Failed to process social callback", e);
                    OAuthUtil.securityFailureForward(request, "Failed to process social callback");
                    return null;
                }

                RealmManager realmManager = new RealmManager(session);
                RealmModel realm = realmManager.getRealm(realmId);

                if (!realm.isEnabled()) {
                    OAuthUtil.securityFailureForward(request, "Realm not enabled.");
                    return null;
                }

                String clientId = requestData.getClientAttributes().get("clientId");

                UserModel client = realm.getUser(clientId);
                if (client == null) {
                    OAuthUtil.securityFailureForward(request, "Unknown login requester.");
                    return null;
                }
                if (!client.isEnabled()) {
                    OAuthUtil.securityFailureForward(request, "Login requester not enabled.");
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
                    OAuthUtil.securityFailureForward(request, "Your account is not enabled.");
                    return null;
                }

                String scope = requestData.getClientAttributes().get("scope");
                String state = requestData.getClientAttributes().get("state");
                String redirectUri = requestData.getClientAttributes().get("redirectUri");

                return OAuthUtil.processAccessCode(realm, tokenManager, authManager, request, uriInfo, scope, state,
                        redirectUri, client, user);
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
            OAuthUtil.securityFailureForward(request, "Social provider not found");
            return null;
        }

        String key = System.getProperty("keycloak.social." + providerId + ".key");
        String secret = System.getProperty("keycloak.social." + providerId + ".secret");
        String callbackUri = callbackUrl(uriInfo).build().toString();

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
            logger.error("Failed to redirect to social auth", t);
            OAuthUtil.securityFailureForward(request, "Failed to redirect to social auth");
            return null;
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
