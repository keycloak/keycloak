package org.keycloak.services.resources.admin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.representations.idm.MappingsRepresentation;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RolePermissionEvaluator;

public interface RoleMappingAwareResource {

    default MappingsRepresentation getAvailableMappings(String scope, String search, Set<String> excludedIds, Integer first, Integer max) {
        Stream<RoleModel> realmRoles = searchRealmRoles(scope, search, excludedIds);
        Stream<RoleModel> clientRoles = searchClientRoles(scope, search, excludedIds == null ? null : excludedIds.stream(), first, max);

        return ModelToRepresentation.toMappingsRepresentation(Stream.concat(realmRoles, clientRoles));
    }

    // roles obtained through composite roles (the children of the direct roles, expanded) and through the given
    // inherited sources (for instance the roles of the groups a user belongs to), expanded as well. A direct role is
    // only part of the result if it is also reachable through one of these paths
    default Stream<RoleModel> getInheritedRoles(Stream<RoleModel> directRoles, Stream<RoleModel> inheritedSources) {
        Set<RoleModel> roots = Stream.concat(directRoles.flatMap(RoleModel::getCompositesStream), inheritedSources)
                .collect(Collectors.toSet());

        return RoleUtils.expandCompositeRoles(roots).stream();
    }

    default Stream<RoleModel> searchClientRoles(String scope, String search, Stream<String> excludedIds, Integer first, Integer max) {
        KeycloakSession session = getSession();
        RealmModel realm = getRealm();
        Set<String> excluded = excludedIds == null ? Collections.emptySet() : excludedIds.collect(Collectors.toSet());
        Stream<RoleModel> result;
        Set<String> ids = canViewAllInScope(scope) ? null : getGrantedClientRoleIds(scope);

        if (ids == null) {
            result = session.roles().searchForClientRolesStream(realm, search, excluded.stream(), null, null);
        } else {
            ids.removeAll(excluded);

            if (ids.isEmpty()) {
                return Stream.empty();
            }

            result = session.roles().searchForClientRolesStream(realm, ids.stream(), search, null, null);
        }

        // the store only narrows the candidates; the per-role check is authoritative so that admin role conflicts and
        // resource-specific permissions overriding resource type ones (FGAP V2) are honored. Paging is applied after
        // it so that pages are not shortened by roles the store returned but the caller cannot see
        result = result.filter(role -> canViewInScope(scope, role));

        if (first != null && first > 0) {
            result = result.skip(first);
        }

        if (max != null && max >= 0) {
            result = result.limit(max);
        }

        return result;
    }

    KeycloakSession getSession();

    AdminPermissionEvaluator getAuth();

    default RealmModel getRealm() {
        return getSession().getContext().getRealm();
    }

    private boolean canViewInScope(String scope, RoleModel role) {
        RolePermissionEvaluator roles = getAuth().roles();

        return switch (scope) {
            case AdminPermissionsSchema.VIEW -> roles.canView(role);
            case AdminPermissionsSchema.MAP_ROLE -> roles.canMapRole(role);
            case AdminPermissionsSchema.MAP_ROLE_COMPOSITE -> roles.canMapComposite(role);
            case AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE -> roles.canMapClientScope(role);
            default -> throw new IllegalArgumentException(scope);
        };
    }

    private Stream<RoleModel> searchRealmRoles(String scope, String search, Set<String> excludedIds) {
        Set<String> excluded = excludedIds == null ? Collections.emptySet() : excludedIds;
        String searchLower = search == null ? null : search.toLowerCase();

        return getRealm().getRolesStream()
                .filter(role -> !excluded.contains(role.getId()))
                .filter(role -> searchLower == null || role.getName().toLowerCase().contains(searchLower))
                .filter(role -> canViewInScope(scope, role));
    }

    private boolean canViewAllInScope(String scope) {
        AdminPermissionEvaluator auth = getAuth();

        return switch (scope) {
            // mirrors canView(RoleModel): the client is viewable or the role can be mapped
            case AdminPermissionsSchema.VIEW -> auth.clients().canView() || canViewAllInScope(AdminPermissionsSchema.MAP_ROLE);
            case AdminPermissionsSchema.MAP_ROLE -> auth.hasOneAdminRole(AdminRoles.MANAGE_USERS);
            case AdminPermissionsSchema.MAP_ROLE_COMPOSITE, AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE -> auth.hasOneAdminRole(AdminRoles.MANAGE_CLIENTS);
            default -> throw new IllegalArgumentException(scope);
        };
    }

    private Set<String> getGrantedClientRoleIds(String scope) {
        AdminPermissionEvaluator auth = getAuth();
        Set<String> ids = new HashSet<>();

        if (AdminPermissionsSchema.VIEW.equals(scope)) {
            // mirrors canView(RoleModel): a role that can be mapped can also be viewed
            Set<String> mappable = getGrantedClientRoleIds(AdminPermissionsSchema.MAP_ROLE);
            if (mappable == null) {
                return null;
            }
            ids.addAll(mappable);
        } else {
            Set<String> roleIds = auth.roles().getRoleIdsByScope(scope);
            // a permission on the resource type itself is reported with the resource type name
            if (roleIds.contains(AdminPermissionsSchema.ROLES_RESOURCE_TYPE)) {
                return null;
            }
            ids.addAll(roleIds);
        }

        Set<String> clientIds = auth.clients().getClientIdsByScope(toClientResourceScope(scope));
        if (clientIds.contains(AdminPermissionsSchema.CLIENTS_RESOURCE_TYPE)) {
            return null;
        }

        for (String clientId : clientIds) {
            ClientModel client = getRealm().getClientById(clientId);
            if (client != null) {
                client.getRolesStream().map(RoleModel::getId).forEach(ids::add);
            }
        }

        return ids;
    }

    // permissions on a client apply to all its roles, so each role scope has a client scope counterpart
    // (for instance, map-roles on a client grants map-role on every role of that client)
    private static String toClientResourceScope(String roleResourceScope) {
        return switch (roleResourceScope) {
            case AdminPermissionsSchema.VIEW -> AdminPermissionsSchema.VIEW;
            case AdminPermissionsSchema.MAP_ROLE -> AdminPermissionsSchema.MAP_ROLES;
            case AdminPermissionsSchema.MAP_ROLE_COMPOSITE -> AdminPermissionsSchema.MAP_ROLES_COMPOSITE;
            case AdminPermissionsSchema.MAP_ROLE_CLIENT_SCOPE -> AdminPermissionsSchema.MAP_ROLES_CLIENT_SCOPE;
            default -> throw new IllegalArgumentException(roleResourceScope);
        };
    }
}
