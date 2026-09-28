package org.keycloak.services.resources.admin;

import java.util.List;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.ui.extend.UiExtensionSupport;

final class UiExtensionPermissions {

    private UiExtensionPermissions() {
    }

    static void requireView(AdminPermissionEvaluator auth, UiExtensionSupport extension) {
        if (hasAnyRole(auth, extension.getRequiredViewRoles())
                || hasAnyRole(auth, extension.getRequiredManageRoles())) {
            return;
        }
        throw new ForbiddenException();
    }

    static void requireManage(AdminPermissionEvaluator auth, UiExtensionSupport extension) {
        requireAnyRole(auth, extension.getRequiredManageRoles(), true);
    }

    private static void requireAnyRole(AdminPermissionEvaluator auth, List<String> roles, boolean manage) {
        if (roles == null || roles.isEmpty()) {
            if (manage) {
                auth.realm().requireManageRealm();
            } else {
                auth.realm().requireViewRealm();
            }
            return;
        }

        if (hasAnyRole(auth, roles)) {
            return;
        }

        throw new ForbiddenException();
    }

    private static boolean hasAnyRole(AdminPermissionEvaluator auth, List<String> roles) {
        return roles != null
                && !roles.isEmpty()
                && roles.stream().anyMatch(role -> auth.hasOneAdminRole(role));
    }
}
