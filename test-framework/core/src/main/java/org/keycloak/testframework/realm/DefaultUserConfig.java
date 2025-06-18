package org.keycloak.testframework.realm;

public class DefaultUserConfig implements UserConfig {

    @Override
    public UserConfigBuilder configure(UserConfigBuilder user) {
        return user;
    }

}
