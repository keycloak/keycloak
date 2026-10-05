package org.keycloak.tests.common;

import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;

/**
 * User configuration compatible with the user user-with-one-configured-otp from testrealm.json from the old arquillian testsuite.
 */
public class UserWithOneConfiguredOtp implements UserConfig {

    public static final String USERNAME = "user-with-one-configured-otp";
    public static final String EMAIL = "otp1@redhat.com";
    public static final String PASSWORD = "password";
    public static final String OTP_SECRET = "DJmQfC73VGFhw7D4QJ8A";

    @Override
    public UserBuilder configure(UserBuilder user) {
        return user.username(USERNAME)
                .password(PASSWORD)
                .name("John", "Doe")
                .email(EMAIL)
                .totpSecret(OTP_SECRET);
    }
}
