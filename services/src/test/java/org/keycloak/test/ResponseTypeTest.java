package org.keycloak.test;

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
        assertFail("id_token");
        assertFail("token");
        assertFail("refresh_token");
        assertSuccess("id_token token");
        assertSuccess("code token");
        assertSuccess("code id_token");
        assertSuccess("code id_token token");
        assertFail("code none");
        assertFail("code refresh_token");
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
