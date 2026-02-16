package org.keycloak.cache;

import java.util.Map;

import org.keycloak.models.ClientModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.provider.Provider;

public interface AlternativeLookupProvider extends Provider {

    IdentityProviderModel lookupIdentityProviderFromIssuer(KeycloakSession session, String issuerUrl);

    ClientModel lookupClientFromClientAttributes(KeycloakSession session, Map<String, String> attributes);

    /**
     * Looks up a role from its string representation, supporting both realm and client roles.
     * <p>
     * The method interprets the {@code roleName} parameter as follows:
     * <ul>
     * <li>For realm roles: the role name directly (e.g., {@code "admin"})</li>
     * <li>For client roles: the format {@code "client-id.role-name"} where the client ID and role name
     *     are separated by a dot separator</li>
     * </ul>
     * <p>
     * Since client IDs can contain dots, the method attempts multiple splits from right to left to resolve ambiguous
     * role names. For example, {@code "my.client.app.role"} will first try to look up
     * client {@code "my.client.app"} with role {@code "role"}, then client {@code "my.client"} with role
     * {@code "app.role"}, and so on.
     * <p>
     * The lookup uses caching to reduce database load. If a role is not found in the cache, the method
     * performs a database lookup and caches the result for subsequent calls.
     *
     * @param realm    the realm in which to look up the role
     * @param roleName the string representation of the role name, which can be a realm role name or a client role in
     *                 the format {@code "client-id.role-name"}. May be {@code null}.
     * @return the corresponding {@link RoleModel} if found, or {@code null} if the role does not exist or if
     * {@code roleName} is {@code null}
     */
    RoleModel lookupRoleFromString(RealmModel realm, String roleName);

}
