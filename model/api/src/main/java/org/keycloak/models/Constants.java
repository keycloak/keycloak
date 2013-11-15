package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Constants {
    String INTERNAL_ROLE = "KEYCLOAK_";
    String ADMIN_REALM = "Keycloak Administration";
    String ADMIN_CONSOLE_APPLICATION = "Admin Console";
    String ADMIN_CONSOLE_ADMIN_ROLE = "admin";
    String APPLICATION_ROLE = INTERNAL_ROLE + "_APPLICATION";
    String IDENTITY_REQUESTER_ROLE = INTERNAL_ROLE + "_IDENTITY_REQUESTER";

    String ACCOUNT_APPLICATION = "Account";
    String ACCOUNT_PROFILE_ROLE = "view-profile";
    String ACCOUNT_MANAGE_ROLE = "manage-account";
}
