package org.keycloak.tests.common;

import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;

/**
 * User configuration compatible with the user user-with-two-configured-otp from testrealm.json from the old arquillian testsuite.
 */
public class UserWithTwoConfiguredOtp implements UserConfig {

    public static final String USERNAME = "user-with-two-configured-otp";
    public static final String EMAIL = "otp2@redhat.com";
    public static final String PASSWORD = "password";
    public static final String OTP_SECRET_ONE = "DJmQfC73VGFhw7D4QJ8A";
    public static final String OTP_SECRET_TWO = "ABCQfC73VGFhw7D4QJ8A";

    @Override
    public UserBuilder configure(UserBuilder user) {
        return user.username(USERNAME)
                .password(PASSWORD)
                .name("John", "Doe")
                .email(EMAIL)
                .totpSecret(OTP_SECRET_ONE)
                .totpSecret(OTP_SECRET_TWO);
    }
}
