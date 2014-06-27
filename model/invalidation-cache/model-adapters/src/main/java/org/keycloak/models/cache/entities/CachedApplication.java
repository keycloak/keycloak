package org.keycloak.models.cache.entities;

import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.cache.KeycloakCache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedApplication extends CachedClient {
    private boolean surrogateAuthRequired;
    private String managementUrl;
    private String baseUrl;
    private List<String> defaultRoles = new LinkedList<String>();
    private boolean bearerOnly;
    private Map<String, String> roles = new HashMap<String, String>();

    public CachedApplication(KeycloakCache cache, ModelProvider delegate, RealmModel realm, ApplicationModel model) {
        super(cache, delegate, realm, model);
        surrogateAuthRequired = model.isSurrogateAuthRequired();
        managementUrl = model.getManagementUrl();
        baseUrl = model.getBaseUrl();
        defaultRoles.addAll(model.getDefaultRoles());
        bearerOnly = model.isBearerOnly();
        for (RoleModel role : model.getRoles()) {
            roles.put(role.getName(), role.getId());
            cache.addCachedRole(new CachedApplicationRole(id, role));
        }


    }

    public boolean isSurrogateAuthRequired() {
        return surrogateAuthRequired;
    }

    public String getManagementUrl() {
        return managementUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public List<String> getDefaultRoles() {
        return defaultRoles;
    }

    public boolean isBearerOnly() {
        return bearerOnly;
    }

    public Map<String, String> getRoles() {
        return roles;
    }

}
