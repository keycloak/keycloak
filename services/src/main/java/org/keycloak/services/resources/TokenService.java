package org.keycloak.services.resources;

import org.jboss.resteasy.jose.Base64Url;
import org.jboss.resteasy.jose.jws.JWSBuilder;
import org.jboss.resteasy.jose.jws.JWSInput;
import org.jboss.resteasy.jose.jws.crypto.RSAProvider;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.jboss.resteasy.logging.Logger;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.SkeletonKeyScope;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.services.managers.AccessCodeEntry;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.TokenManager;
import org.keycloak.services.models.RealmManager;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.RequiredCredentialModel;
import org.keycloak.services.models.ResourceModel;
import org.picketlink.idm.IdentitySession;
import org.picketlink.idm.model.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import java.io.UnsupportedEncodingException;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenService {


    protected static final Logger logger = Logger.getLogger(TokenService.class);
    protected Map<String, AccessCodeEntry> accessCodeMap;

    @Context
    protected UriInfo uriInfo;
    @Context
    protected Providers providers;
    @Context
    protected SecurityContext securityContext;
    @Context
    protected HttpHeaders headers;
    @Context
    protected
    IdentitySession IdentitySession;

    protected RealmModel realm;
    protected TokenManager tokenManager = new TokenManager();
    protected AuthenticationManager authManager = new AuthenticationManager();

    public TokenService(RealmModel realm, Map<String, AccessCodeEntry> accessCodeMap) {
        this.realm = realm;
        this.accessCodeMap = accessCodeMap;
    }

    @Path("grants/identity-token")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response identityTokenGrant(MultivaluedMap<String, String> form) {
        String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            throw new NotAuthorizedException("No user");
        }
        if (!realm.isEnabled()) {
            throw new NotAuthorizedException("Disabled realm");
        }
        User user = realm.getIdm().getUser(username);
        if (user == null) {
            throw new NotAuthorizedException("No user");
        }
        if (!user.isEnabled()) {
            throw new NotAuthorizedException("Disabled user.");
        }
        if (!authManager.authenticateForm(realm, user, form)) {
            throw new NotAuthorizedException("FORM");
        }
        tokenManager = new TokenManager();
        SkeletonKeyToken token = tokenManager.createIdentityToken(realm, username);
        String encoded = tokenManager.encodeToken(realm, token);
        AccessTokenResponse res = accessTokenResponse(token, encoded);
        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Path("grants/access")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response accessTokenGrant(MultivaluedMap<String, String> form) {
        String username = form.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null) {
            throw new NotAuthorizedException("No user");
        }
        if (!realm.isEnabled()) {
            throw new NotAuthorizedException("Disabled realm");
        }
        User user = realm.getIdm().getUser(username);
        if (user == null) {
            throw new NotAuthorizedException("No user");
        }
        if (!user.isEnabled()) {
            throw new NotAuthorizedException("Disabled user.");
        }
        if (authManager.authenticateForm(realm, user, form)) {
            throw new NotAuthorizedException("Auth failed");
        }
        SkeletonKeyToken token = tokenManager.createAccessToken(realm, user);
        String encoded = tokenManager.encodeToken(realm, token);
        AccessTokenResponse res = accessTokenResponse(token, encoded);
        return Response.ok(res, MediaType.APPLICATION_JSON_TYPE).build();
    }

    @Path("auth/request/login")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(MultivaluedMap<String, String> formData) {
        String clientId = formData.getFirst("client_id");
        String scopeParam = formData.getFirst("scope");
        String state = formData.getFirst("state");
        String redirect = formData.getFirst("redirect_uri");

        if (!realm.isEnabled()) {
            return Response.ok("Realm not enabled").type("text/html").build();
        }
        User client = realm.getIdm().getUser(clientId);
        if (client == null) {
            throw new NotAuthorizedException("No client");
        }
        if (!client.isEnabled()) {
            return Response.ok("Requester not enabled").type("text/html").build();
        }
        String username = formData.getFirst("username");
        User user = realm.getIdm().getUser(username);
        if (user == null) {
            logger.debug("user not found");
            return loginForm("Not valid user", redirect, clientId, scopeParam, state, realm, client);
        }
        if (!user.isEnabled()) {
            return Response.ok("Your account is not enabled").type("text/html").build();

        }
        boolean authenticated = authManager.authenticateForm(realm, user, formData);
        if (!authenticated)
            return loginForm("Unable to authenticate, try again", redirect, clientId, scopeParam, state, realm, client);

        SkeletonKeyToken token = null;
        if (scopeParam != null) token = tokenManager.createScopedToken(scopeParam, realm, client, user);
        else token = tokenManager.createLoginToken(realm, client, user);

        AccessCodeEntry code = new AccessCodeEntry();
        code.setExpiration((System.currentTimeMillis() / 1000) + realm.getAccessCodeLifespan());
        code.setToken(token);
        code.setClient(client);
        synchronized (accessCodeMap) {
            accessCodeMap.put(code.getId(), code);
        }
        String accessCode = null;
        try {
            accessCode = new JWSBuilder().content(code.getId().getBytes("UTF-8")).rsa256(realm.getPrivateKey());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        UriBuilder redirectUri = UriBuilder.fromUri(redirect).queryParam("code", accessCode);
        if (state != null) redirectUri.queryParam("state", state);
        return Response.status(302).location(redirectUri.build()).build();
    }

    @Path("access/codes")
    @POST
    @Produces("application/json")
    public Response accessRequest(MultivaluedMap<String, String> formData) {
        if (!realm.isEnabled()) {
            throw new NotAuthorizedException("Realm not enabled");
        }

        String code = formData.getFirst("code");
        if (code == null) {
            logger.debug("code not specified");
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "invalid_request");
            error.put("error_description", "code not specified");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();

        }
        String client_id = formData.getFirst("client_id");
        if (client_id == null) {
            logger.debug("client_id not specified");
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "invalid_request");
            error.put("error_description", "client_id not specified");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }
        User client = realm.getIdm().getUser(client_id);
        if (client == null) {
            logger.debug("Could not find user");
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "invalid_client");
            error.put("error_description", "Could not find user");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }

        if (!client.isEnabled()) {
            logger.debug("user is not enabled");
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "invalid_client");
            error.put("error_description", "User is not enabled");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }

        boolean authenticated = authManager.authenticateForm(realm, client, formData);
        if (!authenticated) {
            Map<String, String> error = new HashMap<String, String>();
            error.put("error", "unauthorized_client");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).type("application/json").build();
        }


        JWSInput input = new JWSInput(code, providers);
        boolean verifiedCode = false;
        try {
            verifiedCode = RSAProvider.verify(input, realm.getPublicKey());
        } catch (Exception ignored) {
            logger.debug("Failed to verify signature", ignored);
        }
        if (!verifiedCode) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Unable to verify code signature");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
        }
        String key = input.readContent(String.class);
        AccessCodeEntry accessCode = null;
        synchronized (accessCodeMap) {
            accessCode = accessCodeMap.remove(key);
        }
        if (accessCode == null) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Code not found");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
        }
        if (accessCode.isExpired()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Code is expired");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
        }
        if (!accessCode.getToken().isActive()) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Token expired");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
        }
        if (!client.getId().equals(accessCode.getClient().getId())) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("error", "invalid_grant");
            res.put("error_description", "Auth error");
            return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON_TYPE).entity(res).build();
        }
        AccessTokenResponse res = accessTokenResponse(realm.getPrivateKey(), accessCode.getToken());
        return Response.ok(res).build();

    }

    protected AccessTokenResponse accessTokenResponse(PrivateKey privateKey, SkeletonKeyToken token) {
        byte[] tokenBytes = null;
        try {
            tokenBytes = JsonSerialization.toByteArray(token, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String encodedToken = new JWSBuilder()
                .content(tokenBytes)
                .rsa256(privateKey);

        return accessTokenResponse(token, encodedToken);
    }

    protected AccessTokenResponse accessTokenResponse(SkeletonKeyToken token, String encodedToken) {
        AccessTokenResponse res = new AccessTokenResponse();
        res.setToken(encodedToken);
        res.setTokenType("bearer");
        if (token.getExpiration() != 0) {
            long time = token.getExpiration() - (System.currentTimeMillis() / 1000);
            res.setExpiresIn(time);
        }
        return res;
    }

    @Path("auth/request")
    @GET
    public Response requestAccessCode(@QueryParam("response_type") String responseType,
                                      @QueryParam("redirect_uri") String redirect,
                                      @QueryParam("client_id") String clientId,
                                      @QueryParam("scope") String scopeParam,
                                      @QueryParam("state") String state) {
        if (!realm.isEnabled()) {
            throw new NotAuthorizedException("Realm not enabled");
        }
        User client = realm.getIdm().getUser(clientId);
        if (client == null)
            return Response.ok("<h1>Security Alert</h1><p>Unknown client trying to get access to your account.</p>").type("text/html").build();

        return loginForm(null, redirect, clientId, scopeParam, state, realm, client);
    }

    private Response loginForm(String validationError, String redirect, String clientId, String scopeParam, String state, RealmModel realm, User client) {
        StringBuffer html = new StringBuffer();
        if (scopeParam != null) {
            html.append("<h1>Grant Request For ").append(realm.getName()).append(" Realm</h1>");
            if (validationError != null) {
                try {
                    Thread.sleep(1000); // put in a delay
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                html.append("<p/><p><b>").append(validationError).append("</b></p>");
            }
            html.append("<p>A Third Party is requesting access to the following resources</p>");
            html.append("<table>");
            SkeletonKeyScope scope = tokenManager.decodeScope(scopeParam);
            Map<String, ResourceModel> resourceMap = realm.getResourceMap();

            for (String res : scope.keySet()) {
                ResourceModel resource = resourceMap.get(res);
                html.append("<tr><td><b>Resource: </b>").append(resource.getName()).append("</td><td><b>Roles:</b>");
                Set<String> scopeMapping = resource.getScope(client);
                for (String role : scope.get(res)) {
                    html.append(" ").append(role);
                    if (!scopeMapping.contains("*") && !scopeMapping.contains(role)) {
                        return Response.ok("<h1>Security Alert</h1><p>Known client not authorized for the requested scope.</p>").type("text/html").build();
                    }
                }
                html.append("</td></tr>");
            }
            html.append("</table><p>To Authorize, please login below</p>");
        } else {
            Set<String> scopeMapping = realm.getScope(client);
            if (scopeMapping.contains("*")) {
                html.append("<h1>Login For ").append(realm.getName()).append(" Realm</h1>");
                if (validationError != null) {
                    try {
                        Thread.sleep(1000); // put in a delay
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    html.append("<p/><p><b>").append(validationError).append("</b></p>");
                }
            } else {
                html.append("<h1>Grant Request For ").append(realm.getName()).append(" Realm</h1>");
                if (validationError != null) {
                    try {
                        Thread.sleep(1000); // put in a delay
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    html.append("<p/><p><b>").append(validationError).append("</b></p>");
                }
                SkeletonKeyScope scope = new SkeletonKeyScope();
                List<ResourceModel> resources = realm.getResources();
                boolean found = false;
                for (ResourceModel resource : resources) {
                    Set<String> resourceScope = resource.getScope(client);
                    if (resourceScope == null) continue;
                    if (resourceScope.size() == 0) continue;
                    if (!found) {
                        found = true;
                        html.append("<p>A Third Party is requesting access to the following resources</p>");
                        html.append("<table>");
                    }
                    html.append("<tr><td><b>Resource: </b>").append(resource.getName()).append("</td><td><b>Roles:</b>");
                    // todo add description of role
                    for (String role : resourceScope) {
                        html.append(" ").append(role);
                        scope.add(resource.getName(), role);
                    }
                }
                if (!found) {
                    return Response.ok("<h1>Security Alert</h1><p>Known client not authorized to access this realm.</p>").type("text/html").build();
                }
                html.append("</table>");
                try {
                    String json = JsonSerialization.toString(scope, false);
                    scopeParam = Base64Url.encode(json.getBytes("UTF-8"));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }

        UriBuilder formActionUri = uriInfo.getBaseUriBuilder().path(TokenService.class).path(TokenService.class, "login");
        String action = formActionUri.build(realm.getId()).toString();
        html.append("<form action=\"").append(action).append("\" method=\"POST\">");
        html.append("Username: <input type=\"text\" name=\"username\" size=\"20\"><br>");

        for (RequiredCredentialModel credential : realm.getRequiredCredentials()) {
            if (!credential.isInput()) continue;
            html.append(credential.getType()).append(": ");
            if (credential.isSecret()) {
                html.append("<input type=\"password\" name=\"").append(credential.getType()).append("\"  size=\"20\"><br>");

            } else {
                html.append("<input type=\"text\" name=\"").append(credential.getType()).append("\"  size=\"20\"><br>");
            }
        }
        html.append("<input type=\"hidden\" name=\"client_id\" value=\"").append(clientId).append("\">");
        if (scopeParam != null) {
            html.append("<input type=\"hidden\" name=\"scope\" value=\"").append(scopeParam).append("\">");
        }
        if (state != null) html.append("<input type=\"hidden\" name=\"state\" value=\"").append(state).append("\">");
        html.append("<input type=\"hidden\" name=\"redirect_uri\" value=\"").append(redirect).append("\">");
        html.append("<input type=\"submit\" value=\"");
        if (scopeParam == null) html.append("Login");
        else html.append("Grant Access");
        html.append("\">");
        html.append("</form>");
        return Response.ok(html.toString()).type("text/html").build();
    }
}
