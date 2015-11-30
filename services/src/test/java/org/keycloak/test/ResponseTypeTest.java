package org.keycloak.test;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ResponseTypeTest {

    @Test
    public void testResponseTypes() {
        assertFail(null);
        assertFail("");
        assertFail("foo");
        assertSuccess("code");
        assertSuccess("none");
        assertSuccess("id_token");
        assertFail("token");
        assertFail("refresh_token");
        assertSuccess("id_token token");
        assertSuccess("code token");
        assertSuccess("code id_token");
        assertSuccess("code id_token token");
        assertFail("code none");
        assertFail("code refresh_token");
    }

    @Test
    public void testMultipleResponseTypes() {
        try {
            OIDCResponseType.parse(Arrays.asList("code", "token"));
            Assert.fail("Not expected to parse with success");
        } catch (IllegalArgumentException iae) {
        }

        OIDCResponseType responseType = OIDCResponseType.parse(Collections.singletonList("code"));
        Assert.assertTrue(responseType.hasResponseType("code"));
        Assert.assertFalse(responseType.hasResponseType("none"));
        Assert.assertFalse(responseType.isImplicitOrHybridFlow());

        responseType = OIDCResponseType.parse(Arrays.asList("code", "none"));
        Assert.assertTrue(responseType.hasResponseType("code"));
        Assert.assertTrue(responseType.hasResponseType("none"));
        Assert.assertFalse(responseType.isImplicitOrHybridFlow());

        responseType = OIDCResponseType.parse(Arrays.asList("code", "code token"));
        Assert.assertTrue(responseType.hasResponseType("code"));
        Assert.assertFalse(responseType.hasResponseType("none"));
        Assert.assertTrue(responseType.hasResponseType("token"));
        Assert.assertFalse(responseType.hasResponseType("id_token"));
        Assert.assertTrue(responseType.isImplicitOrHybridFlow());
        Assert.assertFalse(responseType.isImplicitFlow());

        responseType = OIDCResponseType.parse(Arrays.asList("id_token", "id_token token"));
        Assert.assertFalse(responseType.hasResponseType("code"));
        Assert.assertTrue(responseType.isImplicitOrHybridFlow());
        Assert.assertTrue(responseType.isImplicitFlow());
    }

    private void assertSuccess(String responseType) {
        OIDCResponseType.parse(responseType);
    }

    private void assertFail(String responseType) {
        try {
            OIDCResponseType.parse(responseType);
            Assert.fail("Not expected to parse '" + responseType + "' with success");
        } catch (IllegalArgumentException expected) {
        }
    }
}
