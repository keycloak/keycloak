package org.keycloak.policy;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class PasswordPolicyManagerProviderTest {

    @Test
    public void testValidationWithEmailDelegatesToCustomManager() {
        PolicyError expectedError = new PolicyError("customPasswordPolicyError");
        PasswordPolicyManagerProvider manager = new PasswordPolicyManagerProvider() {
            @Override
            public PolicyError validate(RealmModel realm, UserModel user, String password) {
                return null;
            }

            @Override
            public PolicyError validate(String username, String password) {
                assertEquals("username", username);
                assertEquals("password", password);
                return expectedError;
            }

            @Override
            public void close() {
            }
        };

        PasswordPolicyContext context = PasswordPolicyContext.builder()
                .username("username")
                .email("email@example.com")
                .build();
        assertSame(expectedError, manager.validate(context, "password"));
    }
}
