package org.keycloak.models;

public class ClientRoles {

    public static String CLIENT_ADMIN = "keycloak-client-admin";
    public static String VIEW_CLIENT = "keycloak-client-view";
    public static String MANAGE_CLIENT = "keycloak-client-manage";
    public static String MANAGE_USER_ROLES = "keycloak-client-manage-users";

    public static String[] ALL_ROLES = {CLIENT_ADMIN, VIEW_CLIENT, MANAGE_CLIENT, MANAGE_USER_ROLES};
    public static String[] MANAGEMENT_ROLES = {VIEW_CLIENT, MANAGE_CLIENT, MANAGE_USER_ROLES};
}
