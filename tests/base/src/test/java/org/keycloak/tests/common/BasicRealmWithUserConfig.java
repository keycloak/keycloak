package org.keycloak.tests.common;

import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;

public class BasicRealmWithUserConfig implements RealmConfig {

    public static final String USERNAME =  "basic-user";
    public static final String PASSWORD = "password";

    @Override
    public RealmBuilder configure(RealmBuilder realm) {
        realm.addUser("basic-user").password("password").email("basic@localhost").name("First", "Last");
        return realm;
    }

}
