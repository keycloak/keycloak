package org.keycloak.admin.client;

import org.keycloak.admin.client.resource.KeycloakApplicationResource;
import org.keycloak.representations.idm.RoleRepresentation;

/**
 * Created with IntelliJ IDEA.
 * User: rodrigo
 * Date: 7/10/14
 * Time: 2:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    private static final String SERVER_URL = "http://account.icarros.com/auth";
    private static final String REALM = "icarros";
    private static final String USERNAME = "icarros-admin";
    private static final String PASSWORD = "icarros2000";
    private static final String CLIENT_ID = "icarros-admin-client";
    private static final String CLIENT_SECRET = "b19e2dac-467b-489d-b1e2-fa9aa7606417";


    public static void main(String[] args) {
        Keycloak keycloak = Keycloak.getInstance(SERVER_URL, REALM, USERNAME, PASSWORD, "test-client");

        KeycloakApplicationResource application = keycloak.application("icarros-webapp");
        for(RoleRepresentation role : application.roles().getList()){
            System.out.println(role.getName());
        }

    }

}
