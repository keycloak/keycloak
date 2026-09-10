package org.keycloak.testframework.realm;

public class DefaultUserConfig implements UserConfig {

    @Override
    public UserBuilder configure(UserBuilder user) {
        return user;
    }

}
