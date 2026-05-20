package org.keycloak.tests.common;

import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;

public class BasicUserConfig implements UserConfig {

    @Override
    public UserBuilder configure(UserBuilder user) {
        return user.username("basic-user").password("password").email("basic@localhost").name("First", "Last");
    }

}
