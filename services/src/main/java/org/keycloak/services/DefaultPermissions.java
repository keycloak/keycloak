package org.keycloak.services;

import org.keycloak.Token;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.models.Permissions;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.services.resources.admin.fgap.GroupPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.RealmPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;

import static org.keycloak.authorization.fgap.AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.REALMS_RESOURCE_TYPE;
import static org.keycloak.authorization.fgap.AdminPermissionsSchema.USERS_RESOURCE_TYPE;

public class DefaultPermissions implements Permissions {

    private final KeycloakSession session;
    private final KeycloakContext context;
    private AdminPermissionEvaluator realmAuth;

    public DefaultPermissions(KeycloakSession session, KeycloakContext context) {
        this.session = session;
        this.context = context;
    }

    @Override
    public boolean hasPermission(String resourceType, String scope) {
        return hasPermission(null, resourceType, scope);
    }

    @Override
    public boolean hasPermission(Model model, String realmResourceType, String scope) {
        return switch (realmResourceType) {
            case USERS_RESOURCE_TYPE -> evaluateUserPermission(model, scope);
            case GROUPS_RESOURCE_TYPE -> evaluateGroupPermission(model, scope);
            case REALMS_RESOURCE_TYPE -> evaluateRealmPermission(scope);
            default -> false;
        };
    }

    private boolean evaluateGroupPermission(Model model, String scope) {
        Token token = context.getBearerToken();

        if (token instanceof AccessToken accessToken) {
            AdminPermissionEvaluator evaluator = getEvaluator(accessToken);
            GroupPermissionEvaluator groups = evaluator.groups();

            if (AdminPermissionsSchema.VIEW.equals(scope)) {
                return model == null ? groups.canView() : groups.canView((GroupModel) model);
            } else if (AdminPermissionsSchema.MANAGE.equals(scope)) {
                return model == null ? groups.canManage() : groups.canManage((GroupModel) model);
            } else if (AdminPermissionsSchema.MANAGE_MEMBERSHIP.equals(scope)) {
                return model != null && groups.canManageMembership((GroupModel) model);
            } else if (AdminPermissionsSchema.QUERY.equals(scope)) {
                return groups.canList();
            }
        }

        return false;
    }

    private boolean evaluateUserPermission(Model model, String scope) {
        Token token = context.getBearerToken();

        if (token instanceof AccessToken accessToken) {
            AdminPermissionEvaluator evaluator = getEvaluator(accessToken);
            UserPermissionEvaluator users = evaluator.users();

            if (AdminPermissionsSchema.VIEW.equals(scope)) {
                return model == null ? users.canView() : users.canView((UserModel) model);
            } else if (AdminPermissionsSchema.MANAGE.equals(scope)) {
                return model == null ? users.canManage() : users.canManage((UserModel) model);
            } else if (AdminPermissionsSchema.MANAGE_GROUP_MEMBERSHIP.equals(scope)) {
                return model != null && users.canManageGroupMembership((UserModel) model);
            } else if (AdminPermissionsSchema.QUERY.equals(scope)) {
                return users.canQuery();
            }
        }

        return false;
    }

    private boolean evaluateRealmPermission(String scope) {
        Token token = context.getBearerToken();

        if (token instanceof AccessToken accessToken) {
            AdminPermissionEvaluator evaluator = getEvaluator(accessToken);
            RealmPermissionEvaluator realms = evaluator.realm();

            if (AdminPermissionsSchema.VIEW.equals(scope)) {
                return realms.canViewRealm();
            }
        }

        return false;
    }

    private AdminPermissionEvaluator getEvaluator(AccessToken accessToken) {
        if (realmAuth == null) {
            realmAuth = AdminPermissions.evaluator(session, context.getRealm(), new AdminAuth(context.getRealm(), accessToken, context.getUser(), context.getClient()));
        }
        return realmAuth;
    }
}
