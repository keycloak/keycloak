package org.keycloak.validation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ValidationContextKeyTest {

    @Test
    public void lookup() {

        ValidationContextKey userContext = ValidationContextKey.get("user");
        assertNotNull(userContext);

        ValidationContextKey doesNotExistKey = ValidationContextKey.get("doesnotexist");
        assertNull(doesNotExistKey);
    }

    @Test
    public void getOrCreate() {

        assertEquals(ValidationContextKey.REALM_DEFAULT_CONTEXT_KEY, ValidationContextKey.getOrCreate("realm", null));

        ValidationContextKey custom = ValidationContextKey.getOrCreate("realm.doesnotexist", ValidationContextKey.REALM_DEFAULT_CONTEXT_KEY);
        assertNotNull(custom);
        assertTrue(custom instanceof ValidationContextKey.CustomValidationContextKey);
        assertEquals(ValidationContextKey.REALM_DEFAULT_CONTEXT_KEY, custom.getParent());
    }
}