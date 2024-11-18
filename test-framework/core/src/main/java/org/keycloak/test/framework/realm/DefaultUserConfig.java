package org.keycloak.test.framework.realm;

public class DefaultUserConfig implements UserConfig {

    @Override
    public UserConfigBuilder configure(UserConfigBuilder user) {
        return user;
    }

}
