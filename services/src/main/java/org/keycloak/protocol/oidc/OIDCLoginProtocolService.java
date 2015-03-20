package org.keycloak.protocol.oidc;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ClientConnection;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.RSATokenVerifier;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.LoginStatusIframeEndpoint;
import org.keycloak.protocol.oidc.endpoints.LogoutEndpoint;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.oidc.endpoints.UserInfoEndpoint;
import org.keycloak.protocol.oidc.endpoints.ValidateTokenEndpoint;
import org.keycloak.protocol.oidc.representations.JSONWebKeySet;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.flows.Flows;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource class for the oauth/openid connect token service
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCLoginProtocolService {

    protected static final Logger logger = Logger.getLogger(OIDCLoginProtocolService.class);

    private RealmModel realm;
    private TokenManager tokenManager;
    private EventBuilder event;
    private AuthenticationManager authManager;

    @Context
    private UriInfo uriInfo;

    @Context
    private KeycloakSession session;

    @Context
    private HttpHeaders headers;

    public OIDCLoginProtocolService(RealmModel realm, EventBuilder event, AuthenticationManager authManager) {
        this.realm = realm;
        this.tokenManager = new TokenManager();
        this.event = event;
        this.authManager = authManager;
    }

    public static UriBuilder tokenServiceBaseUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return tokenServiceBaseUrl(baseUriBuilder);
    }

    public static UriBuilder tokenServiceBaseUrl(UriBuilder baseUriBuilder) {
        return baseUriBuilder.path(RealmsResource.class).path("{realm}/protocol/" + OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    public static UriBuilder authUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return authUrl(baseUriBuilder);
    }

    public static UriBuilder authUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "auth");
    }

    public static UriBuilder tokenUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "token");
    }

    public static UriBuilder validateAccessTokenUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "validateAccessToken");
    }

    public static UriBuilder logoutUrl(UriInfo uriInfo) {
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        return logoutUrl(baseUriBuilder);
    }

    public static UriBuilder logoutUrl(UriBuilder baseUriBuilder) {
        UriBuilder uriBuilder = tokenServiceBaseUrl(baseUriBuilder);
        return uriBuilder.path(OIDCLoginProtocolService.class, "logout");
    }

    /**
     * Authorization endpoint
     */
    @Path("auth")
    public Object auth() {
        AuthorizationEndpoint endpoint = new AuthorizationEndpoint(authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.init();
    }

    /**
     * Registration endpoint
     */
    @Path("registrations")
    public Object registerPage() {
        AuthorizationEndpoint endpoint = new AuthorizationEndpoint(authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.init().register();
    }

    /**
     * Token endpoint
     */
    @Path("token")
    public Object token() {
        TokenEndpoint endpoint = new TokenEndpoint(tokenManager, authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.init();
    }

    @Path("login")
    @Deprecated
    public Object loginPage() {
        AuthorizationEndpoint endpoint = new AuthorizationEndpoint(authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.legacy(OIDCLoginProtocol.CODE_PARAM).init();
    }

    @Path("login-status-iframe.html")
    public Object getLoginStatusIframe() {
        LoginStatusIframeEndpoint endpoint = new LoginStatusIframeEndpoint(realm);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @Path("grants/access")
    @Deprecated
    public Object grantAccessToken() {
        TokenEndpoint endpoint = new TokenEndpoint(tokenManager, authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.legacy(OAuth2Constants.PASSWORD).init();
    }

    @Path("refresh")
    @Deprecated
    public Object refreshAccessToken() {
        TokenEndpoint endpoint = new TokenEndpoint(tokenManager, authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.legacy(OAuth2Constants.REFRESH_TOKEN).init();
    }

    @Path("access/codes")
    @Deprecated
    public Object accessCodeToToken() {
        TokenEndpoint endpoint = new TokenEndpoint(tokenManager, authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint.legacy(OAuth2Constants.AUTHORIZATION_CODE).init();
    }

    @Path("validate")
    public Object validateAccessToken(@QueryParam("access_token") String tokenString) {
        ValidateTokenEndpoint endpoint = new ValidateTokenEndpoint(tokenManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;

    }

    @GET
    @Path("certs")
    @Produces(MediaType.APPLICATION_JSON)
    public JSONWebKeySet certs() {
        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(new JWK[]{JWKBuilder.create().rs256(realm.getPublicKey())});
        return keySet;
    }

    @Path("userinfo")
    public Object issueUserInfo() {
        UserInfoEndpoint endpoint = new UserInfoEndpoint(tokenManager, realm);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @Path("logout")
    public Object logout() {
        LogoutEndpoint endpoint = new LogoutEndpoint(tokenManager, authManager, realm, event);
        ResteasyProviderFactory.getInstance().injectProperties(endpoint);
        return endpoint;
    }

    @Path("oauth/oob")
    @GET
    public Response installedAppUrnCallback(final @QueryParam("code") String code, final @QueryParam("error") String error, final @QueryParam("error_description") String errorDescription) {
        LoginFormsProvider forms = Flows.forms(session, realm, null, uriInfo, headers);
        if (code != null) {
            return forms.setClientSessionCode(code).createCode();
        } else {
            return forms.setError(error).createCode();
        }
    }

}
