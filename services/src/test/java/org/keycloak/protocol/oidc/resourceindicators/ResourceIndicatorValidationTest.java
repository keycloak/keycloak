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
    public void testNamespaceSpecificStringAllowsAllPchar() {
        // RFC 8141: NSS = pchar *(pchar / "/"), with pchar from RFC 3986.
        assertValid("urn:example:rootgroup/subgroup/child");
        assertValid("urn:example:foo~bar");
        assertValid("urn:example:foo&bar");
        assertValid("urn:example:a:b@c;d=e,f");
        assertValid("urn:example:foo%2Fbar");
        assertValid("urn:example:foo%2fbar");
        assertValid("urn:example:Foo");
    }

    @Test
    public void testNamespaceSpecificStringRejectsNonPchar() {
        // A slash cannot be the first character of the NSS, and the NSS cannot be empty.
        assertInvalid("urn:example:/foo");
        assertInvalid("urn:example:");
        assertInvalid("urn:example:foo bar");
        assertInvalid("urn:example:foo%zz");
    }

    @Test
    public void testNamespaceIdentifier() {
        // RFC 8141: NID = alphanum 0*30(ldh) alphanum, so two to thirty-two characters
        // starting and ending with an alphanumeric one. The scheme and the NID are
        // both case-insensitive.
        assertValid("URN:example:test");
        assertValid("urn:EXAMPLE:test");
        assertValid("urn:ab:test");
        assertValid("urn:0123456789012345678901234567890a:test");

        assertInvalid("urn:a:test");
        assertInvalid("urn:example-:test");
        assertInvalid("urn:-example:test");
        assertInvalid("urn:0123456789012345678901234567890ab:test");
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
