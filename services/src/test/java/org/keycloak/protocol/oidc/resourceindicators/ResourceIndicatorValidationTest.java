package org.keycloak.protocol.oidc.resourceindicators;

import org.junit.Assert;
import org.junit.Test;

public class ResourceIndicatorValidationTest {

    @Test
    public void testValidResourceIndicatorUrns() {
        assertValid("urn:client:something");
        assertValid("urn:client:asdfasdfs23_asdfasefr43_asdf34f43-asdf34avdrvdr");
        assertValid("urn:something:something");
    }

    @Test
    public void testValidResourceIndicatorUrls() {
        assertValid("https://something");
        assertValid("https://something:8080");
        assertValid("https://something:8080/something");
        assertValid("https://something/something");
    }

    @Test
    public void testInvalidResourceIndicatorUrns() {
        assertInvalid("urn:client:something#something");
        assertInvalid("urn:client:something?foo=bar");
    }

    @Test
    public void testInvalidResourceIndicatorUrls() {
        assertInvalid("https://something#something");
        assertInvalid("https://something?something");
        assertInvalid("/something");
    }

    private void assertValid(String str) {
        Assert.assertTrue(ResourceIndicatorValidation.isValidResourceIndicator(str));
    }

    private void assertInvalid(String str) {
        Assert.assertFalse(ResourceIndicatorValidation.isValidResourceIndicator(str));
    }

}
