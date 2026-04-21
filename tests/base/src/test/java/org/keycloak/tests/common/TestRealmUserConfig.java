package org.keycloak.tests.common;

import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;

/**
 * User configuration compatible with the user test-user@localhost from testrealm.json from the old arquillian testsuite.
 */
public class TestRealmUserConfig implements UserConfig {

    @Override
    public UserConfigBuilder configure(UserConfigBuilder user) {
        return user.username("test-user@localhost")
                .password("password")
                .email("test-user@localhost")
                .name("Tom", "Brady");
    }
}
