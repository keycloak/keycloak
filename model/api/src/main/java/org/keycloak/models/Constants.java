package org.keycloak.models;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface Constants {
    String ADMIN_REALM = "keycloak-admin";
    String ADMIN_CONSOLE_APPLICATION = "admin-console";

    String INTERNAL_ROLE = "KEYCLOAK_";
    String APPLICATION_ROLE = INTERNAL_ROLE + "_APPLICATION";
    String IDENTITY_REQUESTER_ROLE = INTERNAL_ROLE + "_IDENTITY_REQUESTER";

    String ACCOUNT_MANAGEMENT_APP = "account";
}
