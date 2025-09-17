package org.keycloak.authentication.authenticators.client;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class FederatedJWTClientAuthenticatorTest {

    @Test
    public void testToIssuer() {
        Assertions.assertEquals("https://something", FederatedJWTClientAuthenticator.toIssuer("https://something/client"));
        Assertions.assertEquals("spiffe://trust-domain", FederatedJWTClientAuthenticator.toIssuer("spiffe://trust-domain/the"));
        Assertions.assertEquals("spiffe://trust-domain", FederatedJWTClientAuthenticator.toIssuer("spiffe://trust-domain/the/client"));
        Assertions.assertNull(FederatedJWTClientAuthenticator.toIssuer("client"));
        Assertions.assertNull(FederatedJWTClientAuthenticator.toIssuer("the/client"));
        Assertions.assertNull(FederatedJWTClientAuthenticator.toIssuer("spiffe:/the/client"));
    }

}
