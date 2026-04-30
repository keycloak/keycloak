package org.keycloak.tests.common;

import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;

public class BasicRealmWithUserConfig implements RealmConfig {

    public static final String USERNAME =  "basic-user";
    public static final String PASSWORD = "password";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        return realm.users(UserBuilder.create("basic-user").password("password").email("basic@localhost").name("First", "Last"));
    }

}
