package org.keycloak.services.resources.admin;

import java.util.List;

import jakarta.ws.rs.ForbiddenException;

import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.ui.extend.UiExtensionSupport;

final class UiExtensionPermissions {

    private UiExtensionPermissions() {
    }

    static void requireView(AdminPermissionEvaluator auth, UiExtensionSupport extension) {
        requireAnyRole(auth, extension.getRequiredViewRoles(), false);
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

        if (roles.stream().anyMatch(role -> auth.hasOneAdminRole(role))) {
            return;
        }

        throw new ForbiddenException();
    }
}
