package org.keycloak.services.managers;

import org.jboss.resteasy.logging.Logger;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.SkeletonKeyScope;
import org.keycloak.representations.SkeletonKeyToken;
import org.keycloak.util.Base64Url;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
    protected static final Logger logger = Logger.getLogger(TokenManager.class);

    protected Map<String, AccessCodeEntry> accessCodeMap = new ConcurrentHashMap<String, AccessCodeEntry>();

    public void clearAccessCodes() {
        accessCodeMap.clear();
    }

    public AccessCodeEntry getAccessCode(String key) {
        return accessCodeMap.get(key);
    }

    public AccessCodeEntry pullAccessCode(String key) {
        return accessCodeMap.remove(key);
    }

    protected boolean desiresScope(SkeletonKeyScope scope, String key, String roleName) {
        if (scope == null || scope.isEmpty()) return true;
        List<String> val = scope.get(key);
        if (val == null) return false;
        return val.contains(roleName);

    }

    protected boolean desiresScopeGroup(SkeletonKeyScope scope, String key) {
        if (scope == null || scope.isEmpty()) return true;
        return scope.containsKey(key);
    }

    protected boolean isEmpty(SkeletonKeyScope scope) {
        return scope == null || scope.isEmpty();
    }


    public AccessCodeEntry createAccessCode(String scopeParam, String state, String redirect, RealmModel realm, UserModel client, UserModel user) {
        AccessCodeEntry code = new AccessCodeEntry();
        SkeletonKeyScope scopeMap = null;
        if (scopeParam != null) scopeMap = decodeScope(scopeParam);
        List<RoleModel> realmRolesRequested = code.getRealmRolesRequested();
        MultivaluedMap<String, RoleModel> resourceRolesRequested = code.getResourceRolesRequested();

        Set<RoleModel> realmScopeMappings = realm.getRealmScopeMappings(client);
        for (RoleModel desiredRole : realmScopeMappings) {
            if (!realm.hasRole(user, desiredRole)) continue;
            if (desiresScope(scopeMap, "realm", desiredRole.getName())) {
                realmRolesRequested.add(desiredRole);
            }
        }

        for (ApplicationModel application : realm.getApplications()) {
            if (!desiresScopeGroup(scopeMap, application.getName())) continue;
            Set<RoleModel> desiredRoles = application.getApplicationScopeMappings(client);
            if (desiredRoles.isEmpty()) {
                if (application.getApplicationUser().getLoginName().equals(client.getLoginName())) {
                    Set<RoleModel> appRoleMappings = application.getApplicationRoleMappings(user);
                    for (RoleModel desiredAppRole : appRoleMappings) {
                        if (desiresScope(scopeMap, application.getName(), desiredAppRole.getName())) {
                            resourceRolesRequested.add(application.getName(), desiredAppRole);
                        }
                    }
                }
            } else {
                for (RoleModel desiredAppRole : desiredRoles) {
                    if (realm.hasRole(user, desiredAppRole) && desiresScope(scopeMap, application.getName(), desiredAppRole.getName())) {
                        resourceRolesRequested.add(application.getName(), desiredAppRole);
                    }
                }
            }
        }

        createToken(code, realm, client, user);
        code.setRealm(realm);
        code.setExpiration((System.currentTimeMillis() / 1000) + realm.getAccessCodeLifespan());
        code.setClient(client);
        code.setUser(user);
        code.setState(state);
        code.setRedirectUri(redirect);
        accessCodeMap.put(code.getId(), code);
        String accessCode = null;
        try {
            accessCode = new JWSBuilder().content(code.getId().getBytes("UTF-8")).rsa256(realm.getPrivateKey());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        code.setCode(accessCode);
        return code;
    }

    protected SkeletonKeyToken initToken(RealmModel realm, UserModel client, UserModel user) {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id(RealmManager.generateId());
        token.principal(user.getLoginName());
        token.audience(realm.getName());
        token.issuedNow();
        token.issuedFor(client.getLoginName());
        if (realm.getTokenLifespan() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + realm.getTokenLifespan());
        }
        Set<String> allowedOrigins = client.getWebOrigins();
        if (allowedOrigins != null) {
            token.setAllowedOrigins(allowedOrigins);
        }
        return token;
    }

    protected void addComposites(SkeletonKeyToken token, RoleModel role) {
        SkeletonKeyToken.Access access = null;
        if (role.getContainer() instanceof RealmModel) {
            access = token.getRealmAccess();
            if (token.getRealmAccess() == null) {
                access = new SkeletonKeyToken.Access();
                token.setRealmAccess(access);
            } else if (token.getRealmAccess().getRoles() != null && token.getRealmAccess().isUserInRole(role.getName()))
                return;

        } else {
            ApplicationModel app = (ApplicationModel) role.getContainer();
            access = token.getResourceAccess(app.getName());
            if (access == null) {
                access = token.addAccess(app.getName());
                if (app.isSurrogateAuthRequired()) access.verifyCaller(true);
            } else if (access.isUserInRole(role.getName())) return;

        }
        access.addRole(role.getName());
        if (!role.isComposite()) return;

        for (RoleModel composite : role.getComposites()) {
            addComposites(token, composite);
        }

    }

    protected void createToken(AccessCodeEntry accessCodeEntry, RealmModel realm, UserModel client, UserModel user) {

        SkeletonKeyToken token = initToken(realm, client, user);

        if (accessCodeEntry.getRealmRolesRequested().size() > 0) {
            SkeletonKeyToken.Access access = new SkeletonKeyToken.Access();
            for (RoleModel role : accessCodeEntry.getRealmRolesRequested()) {
                addComposites(token, role);
            }
        }

        if (accessCodeEntry.getResourceRolesRequested().size() > 0) {
            for (List<RoleModel> roles : accessCodeEntry.getResourceRolesRequested().values()) {
                for (RoleModel role : roles) {
                    addComposites(token, role);
                }
            }
        }
        accessCodeEntry.setToken(token);
    }

    public String encodeScope(SkeletonKeyScope scope) {
        String token = null;
        try {
            token = JsonSerialization.writeValueAsString(scope);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Base64Url.encode(token.getBytes());
    }

    public SkeletonKeyScope decodeScope(String scopeParam) {
        SkeletonKeyScope scope = null;
        byte[] bytes = Base64Url.decode(scopeParam);
        try {
            scope = JsonSerialization.readValue(bytes, SkeletonKeyScope.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return scope;
    }


    public SkeletonKeyToken createAccessToken(RealmModel realm, UserModel user) {
        SkeletonKeyToken token = new SkeletonKeyToken();
        token.id(RealmManager.generateId());
        token.issuedNow();
        token.principal(user.getLoginName());
        token.audience(realm.getName());
        if (realm.getTokenLifespan() > 0) {
            token.expiration((System.currentTimeMillis() / 1000) + realm.getTokenLifespan());
        }
        for (RoleModel role : realm.getRoleMappings(user)) {
            addComposites(token, role);
        }
        return token;
    }


    public String encodeToken(RealmModel realm, Object token) {
        String encodedToken = new JWSBuilder()
                .jsonContent(token)
                .rsa256(realm.getPrivateKey());
        return encodedToken;
    }
}
