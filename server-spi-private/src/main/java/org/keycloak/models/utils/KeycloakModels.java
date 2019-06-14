package org.keycloak.models.utils;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Project Name: keycloak
 * Package Name: org.keycloak.models.utils
 *
 * @author：Liubinwang
 * @date：2018/9/27 17:08
 * Copyright (c) 2018,higovnet.net All Rights Reserved.
 */
public class KeycloakModels {

    /**
     * Try to find user by username or email for authentication
     *
     * @param realm  realm
     * @param idcard
     * @return found user
     */
    public static UserModel findUserByIdcard(KeycloakSession session, RealmModel realm, String idcard) {
        return session.users().getUserByIdcard(idcard, realm);
    }

    /**
     * Try to find user by username or email for authentication
     *
     * @param realm    realm
     * @param username username and idcard
     * @return found user
     */
    public static UserModel findUserByNameAndIdcard(KeycloakSession session, RealmModel realm, String username) {
        if (username.indexOf(" ") != -1) {
            String[] usernames = username.split(" ");
            String firstname = usernames[0];
            String idcard = usernames[1];
            UserModel user = session.users().getUserByIdcard(idcard, realm);
            if (user != null && user.getFirstName().equalsIgnoreCase(firstname)) {
                return user;
            }
        }
        return session.users().getUserByUsername(username, realm);
    }

}
