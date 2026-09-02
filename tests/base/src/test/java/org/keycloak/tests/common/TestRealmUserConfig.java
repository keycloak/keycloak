package org.keycloak.tests.common;

import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;

/**
 * User configuration compatible with the user test-user@localhost from testrealm.json from the old arquillian testsuite.
 */
public class TestRealmUserConfig implements UserConfig {

    @Override
    public UserBuilder configure(UserBuilder user) {
        return user.username("test-user@localhost")
                .password("password")
                .email("test-user@localhost")
                .name("Tom", "Brady");
    }
}
