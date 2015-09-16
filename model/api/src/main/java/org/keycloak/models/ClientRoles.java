package org.keycloak.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientRoles {

    public static String CLIENT_ADMIN = "keycloak-client-admin";
    public static String VIEW_CLIENT = "keycloak-client-view";
    public static String MANAGE_CLIENT = "keycloak-client-manage";
    public static String MANAGE_USER_ROLES = "keycloak-client-manage-users";

    public static List<String> ALL_ROLES = new ArrayList<>(Arrays.asList(CLIENT_ADMIN, VIEW_CLIENT, MANAGE_CLIENT, MANAGE_USER_ROLES));
    public static List<String> MANAGEMENT_ROLES = new ArrayList<>(Arrays.asList(VIEW_CLIENT, MANAGE_CLIENT, MANAGE_USER_ROLES));
}
