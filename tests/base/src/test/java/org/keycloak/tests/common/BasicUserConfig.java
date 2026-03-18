package org.keycloak.tests.common;

import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

public class BasicUserConfig implements UserConfig {

    @Override
    public UserConfigBuilder configure(UserConfigBuilder user) {
        return user.username("basic-user").password("password").email("basic@localhost").name("First", "Last");
    }

}
