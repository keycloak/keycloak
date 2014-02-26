package org.keycloak.models;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AdminRoles {

    public static String APP_SUFFIX = "-realm";

    public static String ADMIN = "admin";

    public static String VIEW_REALM = "view-realm";
    public static String VIEW_USERS = "view-users";
    public static String VIEW_APPLICATIONS = "view-applications";
    public static String VIEW_CLIENTS = "view-clients";

    public static String MANAGE_REALM = "manage-realm";
    public static String MANAGE_USERS = "manage-users";
    public static String MANAGE_APPLICATIONS = "manage-applications";
    public static String MANAGE_CLIENTS = "manage-clients";

    public static String[] ALL_REALM_ROLES = {VIEW_REALM, VIEW_USERS, VIEW_APPLICATIONS, VIEW_CLIENTS, MANAGE_REALM, MANAGE_USERS, MANAGE_APPLICATIONS, MANAGE_CLIENTS};

    public static String getAdminApp(RealmModel realm) {
        return realm.getName() + APP_SUFFIX;
    }

}
