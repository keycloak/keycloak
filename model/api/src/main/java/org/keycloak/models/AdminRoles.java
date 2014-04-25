package org.keycloak.models;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdminRoles {

    public static String APP_SUFFIX = "-realm";

    public static String ADMIN = "admin";

    public static String CREATE_REALM = "create-realm";

    public static String VIEW_REALM = "view-realm";
    public static String VIEW_USERS = "view-users";
    public static String VIEW_APPLICATIONS = "view-applications";
    public static String VIEW_CLIENTS = "view-clients";
    public static String VIEW_AUDIT = "view-audit";

    public static String MANAGE_REALM = "manage-realm";
    public static String MANAGE_USERS = "manage-users";
    public static String MANAGE_APPLICATIONS = "manage-applications";
    public static String MANAGE_CLIENTS = "manage-clients";
    public static String MANAGE_AUDIT = "manage-audit";

    public static String[] ALL_REALM_ROLES = {VIEW_REALM, VIEW_USERS, VIEW_APPLICATIONS, VIEW_CLIENTS, VIEW_AUDIT, MANAGE_REALM, MANAGE_USERS, MANAGE_APPLICATIONS, MANAGE_CLIENTS, MANAGE_AUDIT};

}
