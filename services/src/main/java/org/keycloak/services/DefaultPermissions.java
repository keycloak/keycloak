package org.keycloak.services;

import org.keycloak.Token;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.Model;
import org.keycloak.models.Permissions;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;

public class DefaultPermissions implements Permissions {

    private final KeycloakSession session;
    private final KeycloakContext context;

    public DefaultPermissions(KeycloakSession session, KeycloakContext context) {
        this.session = session;
        this.context = context;
    }

    @Override
    public boolean hasPermission(String resourceType, String scope) {
        Token token = context.getBearerToken();

        if (token instanceof AccessToken accessToken) {
            RealmModel realm = getRealm();
            AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, new AdminAuth(realm, accessToken, context.getUser(), context.getClient()));

            return switch (resourceType) {
                case AdminPermissionsSchema.USERS_RESOURCE_TYPE -> {
                    if (AdminPermissionsSchema.VIEW.equals(scope)) {
                        yield realmAuth.users().canView();
                    } else if (AdminPermissionsSchema.MANAGE.equals(scope)) {
                        yield realmAuth.users().canManage();
                    }
                    yield false;
                }
                case AdminPermissionsSchema.GROUPS_RESOURCE_TYPE -> {
                    if (AdminPermissionsSchema.VIEW.equals(scope)) {
                        yield realmAuth.groups().canView();
                    } else if (AdminPermissionsSchema.MANAGE.equals(scope)) {
                        yield realmAuth.groups().canManage();
                    }
                    yield false;
                }
                case AdminPermissionsSchema.REALMS_RESOURCE_TYPE -> {
                    if (AdminPermissionsSchema.VIEW.equals(scope)) {
                        yield realmAuth.realm().canViewRealm();
                    }
                    yield false;
                }
                default -> false;
            };
        }

        return false;
    }

    @Override
    public boolean hasPermission(Model model, String realmResourceType, String scope) {
        Token token = context.getBearerToken();

        if (token instanceof AccessToken accessToken) {
            RealmModel realm = getRealm();
            AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, new AdminAuth(realm, accessToken, context.getUser(), context.getClient()));

            return switch (realmResourceType) {
                case AdminPermissionsSchema.USERS_RESOURCE_TYPE -> {
                    if (AdminPermissionsSchema.VIEW.equals(scope)) {
                        yield realmAuth.users().canView((UserModel) model);
                    } else if (AdminPermissionsSchema.MANAGE.equals(scope)) {
                        yield realmAuth.users().canManage((UserModel) model);
                    }
                    yield false;
                }
                case AdminPermissionsSchema.GROUPS_RESOURCE_TYPE -> {
                    if (AdminPermissionsSchema.VIEW.equals(scope)) {
                        yield realmAuth.groups().canView((GroupModel) model);
                    } else if (AdminPermissionsSchema.MANAGE.equals(scope)) {
                        yield realmAuth.groups().canManage((GroupModel) model);
                    }
                    yield false;
                }
                default -> false;
            };
        }

        return false;
    }

    @Override
    public boolean canQuery(String resourceType) {
        Token token = context.getBearerToken();

        if (token instanceof AccessToken accessToken) {
            RealmModel realm = getRealm();
            AdminPermissionEvaluator realmAuth = AdminPermissions.evaluator(session, realm, new AdminAuth(realm, accessToken, context.getUser(), context.getClient()));

            return switch (resourceType) {
                case AdminPermissionsSchema.USERS_RESOURCE_TYPE -> realmAuth.users().canQuery();
                case AdminPermissionsSchema.GROUPS_RESOURCE_TYPE -> realmAuth.groups().canList();
                default -> false;
            };
        }

        return false;
    }

    private RealmModel getRealm() {
        return context.getRealm();
    }
}
