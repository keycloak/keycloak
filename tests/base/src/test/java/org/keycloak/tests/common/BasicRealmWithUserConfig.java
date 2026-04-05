package org.keycloak.tests.common;

import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;

public class BasicRealmWithUserConfig implements RealmConfig {

    public static final String USERNAME =  "basic-user";
    public static final String PASSWORD = "password";

    @Override
    public RealmConfigBuilder configure(RealmConfigBuilder realm) {
        realm.addUser("basic-user").password("password").email("basic@localhost").name("First", "Last");
        return realm;
    }

}
