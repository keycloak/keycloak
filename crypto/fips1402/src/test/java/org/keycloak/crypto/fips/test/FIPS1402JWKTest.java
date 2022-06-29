package org.keycloak.crypto.fips.test;

import org.junit.Ignore;
import org.keycloak.jose.jwk.JWKTest;

/**
 * Test with fips1402 security provider and bouncycastle-fips
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Ignore("Ignored by default as it does not work on non-fips enabled environment") // TODO: Figure how to test in the FIPS environments, but still keep disabled in the non-FIPS environments
public class FIPS1402JWKTest extends JWKTest {
}
