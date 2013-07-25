package org.keycloak.services.managers;

import org.jboss.resteasy.jose.Base64Url;
import org.jboss.resteasy.jose.jws.JWSBuilder;
import org.jboss.resteasy.jwt.JsonSerialization;
import org.keycloak.representations.SkeletonKeyScope;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.services.models.RealmModel;
import org.keycloak.services.models.ResourceModel;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.TokenService;
import org.picketlink.idm.model.User;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateful object that creates tokens and manages oauth access codes
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class TokenManager {

    public static final String KEYCLOAK_IDENTITY_COOKIE = "KEYCLOAK_IDENTITY";
    protected Map<String, AccessCodeEntry> accessCodeMap = new ConcurrentHashMap<String, AccessCodeEntry>();

    public void clearAccessCodes() {
        accessCodeMap.clear();
    }

    public AccessCodeEntry pullAccessCode(String key) {
        return accessCodeMap.remove(key);
    }

    public NewCookie createLoginCookie(RealmModel realm, User user, UriInfo uriInfo) {
        SkeletonKeyToken identityToken = createIdentityToken(realm, user.getLoginName());
        String encoded = encodeToken(realm, identityToken);
        URI uri = RealmsResource.realmBaseUrl(uriInfo).build(realm.getId());
        boolean secureOnly = !realm.isSslNotRequired();
        NewCookie cookie = new NewCookie(KEYCLOAK_IDENTITY_COOKIE, encoded, uri.getPath(), null, null, realm.getTokenLifespan(), secureOnly, true);
        return cookie;
    }

    public String createAccessCode(String scopeParam, RealmModel realm, User client, User user)
    {
        SkeletonKeyToken token = null;
        if (scopeParam != null) token = createScopedToken(scopeParam, realm, client, user);
        else token = createLoginToken(realm, client, user);

        AccessCodeEntry code = new AccessCodeEntry();
        code.setExpiration((System.currentTimeMillis() / 1000) + realm.getAccessCodeLifespan());
        code.setToken(token);
        code.setClient(client);
        accessCodeMap.put(code.getId(), code);
        String accessCode = null;
        try {
            accessCode = new JWSBuilder().content(code.getId().getBytes("UTF-8")).rsa256(realm.getPrivateKey());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return accessCode;
    }

    public SkeletonKeyToken createScopedToken(SkeletonKeyScope scope, RealmModel realm, User client, User user) {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id(RealmManager.generateId());
        token.principal(user.getLoginName());
        token.audience(realm.getName());
        token.issuedNow();
        token.issuedFor(client.getLoginName());
        if (realm.getTokenLifespan() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + realm.getTokenLifespan());
        }
        Map<String, ResourceModel> resourceMap = realm.getResourceMap();

        for (String res : scope.keySet()) {
            ResourceModel resource = resourceMap.get(res);
            Set<String> scopeMapping = resource.getScope(client);
            Set<String> roleMapping = resource.getRoleMappings(user);
            SkeletonKeyToken.Access access = token.addAccess(resource.getName());
            for (String role : scope.get(res)) {
                if (!scopeMapping.contains("*") && !scopeMapping.contains(role)) {
                    throw new ForbiddenException(Response.status(403).entity("<h1>Security Alert</h1><p>Known client not authorized for the requested scope.</p>").type("text/html").build());
                }
                if (!roleMapping.contains(role)) {
                    throw new ForbiddenException(Response.status(403).entity("<h1>Security Alert</h1><p>Known client not authorized for the requested scope.</p>").type("text/html").build());

                }
                access.addRole(role);
            }
        }
        return token;
    }

    public SkeletonKeyToken createScopedToken(String scopeParam, RealmModel realm, User client, User user) {
        SkeletonKeyScope scope = decodeScope(scopeParam);
        return createScopedToken(scope, realm, client, user);
    }

    public SkeletonKeyToken createLoginToken(RealmModel realm, User client, User user) {
        Set<String> mapping = realm.getScopes(client);
        if (!mapping.contains("*")) {
            throw new ForbiddenException(Response.status(403).entity("<h1>Security Alert</h1><p>Known client not authorized to request a user login.</p>").type("text/html").build());
        }
        SkeletonKeyToken token = createAccessToken(realm, user);
        token.issuedFor(client.getLoginName());
        return token;

    }


    public String encodeScope(SkeletonKeyScope scope) {
        String token = null;
        try {
            token = JsonSerialization.toString(scope, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Base64Url.encode(token.getBytes());
    }

    public SkeletonKeyScope decodeScope(String scopeParam) {
        SkeletonKeyScope scope = null;
        byte[] bytes = Base64Url.decode(scopeParam);
        try {
            scope = JsonSerialization.fromBytes(SkeletonKeyScope.class, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return scope;
    }


    public SkeletonKeyToken createAccessToken(RealmModel realm, User user) {
        List<ResourceModel> resources = realm.getResources();
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id(RealmManager.generateId());
        token.issuedNow();
        token.principal(user.getLoginName());
        token.audience(realm.getId());
        if (realm.getTokenLifespan() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + realm.getTokenLifespan());
        }

        Set<String> realmMapping = realm.getRoleMappings(user);

        if (realmMapping != null && realmMapping.size() > 0) {
            SkeletonKeyToken.Access access = new SkeletonKeyToken.Access();
            for (String role : realmMapping) {
                access.addRole(role);
            }
            token.setRealmAccess(access);
        }
        if (resources != null) {
            for (ResourceModel resource : resources) {
                Set<String> mapping = resource.getRoleMappings(user);
                if (mapping == null) continue;
                SkeletonKeyToken.Access access = token.addAccess(resource.getName())
                        .verifyCaller(resource.isSurrogateAuthRequired());
                for (String role : mapping) {
                    access.addRole(role);
                }
            }
        }
        return token;
    }

    public SkeletonKeyToken createIdentityToken(RealmModel realm, String username) {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id(RealmManager.generateId());
        token.issuedNow();
        token.principal(username);
        token.audience(realm.getId());
        if (realm.getTokenLifespan() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + realm.getTokenLifespan());
        }
        return token;
    }

    public String encodeToken(RealmModel realm, SkeletonKeyToken token) {
        byte[] tokenBytes = null;
        try {
            tokenBytes = JsonSerialization.toByteArray(token, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String encodedToken = new JWSBuilder()
                .content(tokenBytes)
                .rsa256(realm.getPrivateKey());
        return encodedToken;
    }
}
