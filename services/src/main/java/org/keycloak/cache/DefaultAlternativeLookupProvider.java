package org.keycloak.cache;

import java.util.List;
import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderQuery;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import org.jboss.logging.Logger;

import static org.keycloak.models.utils.KeycloakModelUtils.CLIENT_ROLE_SEPARATOR;
import static org.keycloak.models.utils.KeycloakModelUtils.MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE;

public class DefaultAlternativeLookupProvider implements AlternativeLookupProvider {

    private static final Logger logger = Logger.getLogger(DefaultAlternativeLookupProvider.class);
    private final LocalCache<String, CachedValue> lookupCache;

    DefaultAlternativeLookupProvider(LocalCache<String, CachedValue> lookupCache) {
        this.lookupCache = lookupCache;
    }

    @Override
    public IdentityProviderModel lookupIdentityProviderFromIssuer(KeycloakSession session, String issuerUrl) {
        String alternativeKey = ComputedKey.computeKey(session.getContext().getRealm().getId(), "idp", issuerUrl);

        CachedValue cachedIdpAlias = lookupCache.get(alternativeKey);
        if (cachedIdpAlias instanceof CachedValue.CachedString cachedString) {
            IdentityProviderModel idp = session.identityProviders().getByAlias(cachedString.value());
            if (idp != null && issuerUrl.equals(idp.getConfig().get(IdentityProviderModel.ISSUER)) && idp.isEnabled()) {
                return idp;
            } else {
                lookupCache.invalidate(alternativeKey);
            }
        }

        List<IdentityProviderModel> idps = session.identityProviders().getAllStream(IdentityProviderQuery.any())
                .filter(i -> issuerUrl.equals(i.getConfig().get(IdentityProviderModel.ISSUER)) && i.isEnabled())
                .limit(2)
                .toList();
        IdentityProviderModel idp = null;
        if (idps.size() == 1) {
            idp = idps.get(0);
            if (idp.getAlias() != null) {
                lookupCache.put(alternativeKey, CachedValue.ofId(idp.getAlias()));
            }
        } else if (idps.size() > 1) {
            throw new RuntimeException("Multiple IDPs match the same issuer: " + idps.stream().map(IdentityProviderModel::getAlias).toList());
        }

        return idp;
    }

    @Override
    public ClientModel lookupClientFromClientAttributes(KeycloakSession session, Map<String, String> attributes) {
        String alternativeKey = ComputedKey.computeKey(session.getContext().getRealm().getId(), "client", attributes);

        CachedValue cachedClientId = lookupCache.get(alternativeKey);
        if (cachedClientId instanceof CachedValue.CachedString cachedString) {
            ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), cachedString.value());
            boolean match = client != null && client.isEnabled();
            if (match) {
                for (Map.Entry<String, String> e : attributes.entrySet()) {
                    if (!e.getValue().equals(client.getAttribute(e.getKey()))) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                return client;
            } else {
                lookupCache.invalidate(alternativeKey);
            }
        }

        ClientModel client = null;
        List<ClientModel> clients = session.clients().searchClientsByAttributes(session.getContext().getRealm(), attributes, null, null)
                .filter(ClientModel::isEnabled)
                .limit(2)
                .toList();
        if (clients.size() == 1) {
            client = clients.get(0);
            lookupCache.put(alternativeKey, CachedValue.ofId(client.getClientId()));
        } else if (clients.size() > 1) {
            throw new RuntimeException("Multiple clients matches attributes");
        }

        return client;
    }

    @Override
    public RoleModel lookupRoleFromString(RealmModel realm, String roleName) {
        if (roleName == null) {
            return null;
        }

        var roleModel = findRoleInCache(realm, roleName);
        if (roleModel != null) {
            return roleModel;
        }

        // Check client roles for all possible splits by dot
        int counter = 0;
        int scopeIndex = roleName.lastIndexOf(CLIENT_ROLE_SEPARATOR);
        while (scopeIndex >= 0 && counter < MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE) {
            counter++;
            String appName = roleName.substring(0, scopeIndex);
            ClientModel client = realm.getClientByClientId(appName);
            if (client != null) {
                return storeClientRoleInCache(client, cachedRoleKey(realm, roleName), roleName.substring(scopeIndex + 1), counter);
            }

            scopeIndex = roleName.lastIndexOf(CLIENT_ROLE_SEPARATOR, scopeIndex - 1);
        }
        if (counter >= MAX_CLIENT_LOOKUPS_DURING_ROLE_RESOLVE) {
            logger.warnf("Not able to retrieve role model from the role name '%s'. Please use shorter role names with the limited amount of dots, roleName", roleName.length() > 100 ? roleName.substring(0, 100) + "..." : roleName);
            return null;
        }

        return storeRealmRoleInCache(realm, roleName);
    }

    @Override
    public void close() {
    }

    private RoleModel findRoleInCache(RealmModel realm, String roleName) {
        var cacheKey = cachedRoleKey(realm, roleName);
        var cachedRole = lookupCache.get(cacheKey);
        if (!(cachedRole instanceof CachedValue.CachedRoleQualifier cachedRoleQualifier)) {
            return null;
        }
        if (cachedRoleQualifier.isRealmRole()) {
            var role = realm.getRole(cachedRoleQualifier.roleName());
            if (role == null) {
                lookupCache.invalidate(cacheKey);
            }
            return role;
        }

        var client = realm.getClientByClientId(cachedRoleQualifier.clientId());
        if (client == null) {
            lookupCache.invalidate(cacheKey);
            return null;
        }

        var role = client.getRole(cachedRoleQualifier.roleName());
        if (role == null) {
            lookupCache.invalidate(cacheKey);
        }
        return role;
    }

    private RoleModel storeClientRoleInCache(ClientModel client, String cacheKey, String roleName, int dotCount) {
        // If dotCount is equals to 1, we skip caching.
        // It means, we have the following format, client-id.role-name.
        // Both realm.getClientByClientId and client.getRole methods already use an internal cache.
        var roleModel = client.getRole(roleName);
        if (roleModel != null && dotCount > 1) {
            lookupCache.put(cacheKey, CachedValue.ofClientRole(client.getClientId(), roleName));
        }
        return roleModel;
    }

    private RoleModel storeRealmRoleInCache(RealmModel realm, String roleName) {
        // determine if roleName is a realm role
        var roleModel = realm.getRole(roleName);
        if (roleModel != null) {
            // only cache if the role is present
            lookupCache.put(cachedRoleKey(realm, roleName), CachedValue.ofRealmRole(roleName));
        }
        return roleModel;
    }

    private static String cachedRoleKey(RealmModel realm, String roleName) {
        return realm.getId() + roleName;
    }
}
