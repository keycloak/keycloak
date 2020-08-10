package org.keycloak.validation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ValidationKeyTest {

    @Test
    public void lookup() {

        ValidationKey usernameKey = ValidationKey.get("user.username");
        assertNotNull(usernameKey);

        ValidationKey doesNotExistKey = ValidationKey.get("user.doesnotexist");
        assertNull(doesNotExistKey);
    }

    @Test
    public void getOrCreate() {

        assertEquals(ValidationKey.USER_USERNAME, ValidationKey.getOrCreate("user.username"));

        ValidationKey customKey = ValidationKey.getOrCreate("user.doesnotexist");
        assertNotNull(customKey);
        assertTrue(customKey instanceof ValidationKey.CustomValidationKey);
    }
}