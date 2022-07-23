package org.keycloak.crypto.fips.test;

import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.jose.jwk.JWKTest;

/**
 * Test with fips1402 security provider and bouncycastle-fips
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FIPS1402JWKTest extends JWKTest {

    @Ignore("Test not supported by BC FIPS")
    @Test
    public void publicEs256() throws Exception {
        // Do nothing
    }

}
