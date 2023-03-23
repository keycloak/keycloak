package org.keycloak.admin.ui.rest;

import static org.keycloak.admin.ui.rest.model.RoleMapper.convertToModel;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.keycloak.admin.ui.rest.model.ClientRole;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public abstract class RoleMappingResource {
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;

    public RoleMappingResource(RealmModel realm, AdminPermissionEvaluator auth) {
        this.realm = realm;
        this.auth = auth;
    }

    protected final Stream<ClientRole> mapping(Predicate<RoleModel> predicate) {
        return realm.getClientsStream().flatMap(RoleContainerModel::getRolesStream).filter(predicate)
                .filter(auth.roles()::canMapRole).map(roleModel -> convertToModel(roleModel, realm.getClientsStream()));
    }

    protected final Stream<ClientRole> mapping(Predicate<RoleModel> predicate, Predicate<RoleModel> authPredicate) {
        return realm.getClientsStream().flatMap(RoleContainerModel::getRolesStream).filter(predicate)
                .filter(authPredicate).map(roleModel -> convertToModel(roleModel, realm.getClientsStream()));
    }

    protected final List<ClientRole> mapping(Predicate<RoleModel> predicate, long first, long max, final String search) {
        return mapping(predicate).filter(clientRole -> clientRole.getClient().contains(search) || clientRole.getRole().contains(search))
                .skip(first).limit(max).collect(Collectors.toList());
    }

    protected final List<ClientRole> mapping(Predicate<RoleModel> predicate, Predicate<RoleModel> authPredicate, long first, long max, final String search) {
        return mapping(predicate, authPredicate).filter(clientRole -> clientRole.getClient().contains(search) || clientRole.getRole().contains(search))
                .skip(first).limit(max).collect(Collectors.toList());
    }
}
