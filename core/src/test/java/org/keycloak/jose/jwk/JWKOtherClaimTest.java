package org.keycloak.jose.jwk;

import java.util.ArrayList;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link JWK#getOtherClaim(String, Class)} type safety.
 * <p>
 * Verifies that the method gracefully handles type mismatches in the otherClaims map
 * instead of throwing {@link ClassCastException}.
 */
public class JWKOtherClaimTest {

    @Test
    public void getOtherClaimReturnsValueWhenTypeMatches() {
        JWK jwk = new JWK();
        jwk.setOtherClaims("crv", "P-256");

        String result = jwk.getOtherClaim("crv", String.class);

        assertEquals("P-256", result);
    }

    @Test
    public void getOtherClaimReturnsNullWhenClaimIsAbsent() {
        JWK jwk = new JWK();

        String result = jwk.getOtherClaim("crv", String.class);

        assertNull(result);
    }

    @Test
    public void getOtherClaimReturnsNullWhenValueIsWrongType() {
        JWK jwk = new JWK();
        jwk.setOtherClaims("crv", new ArrayList<>());

        String result = jwk.getOtherClaim("crv", String.class);

        assertNull(result);
    }

    @Test
    public void getOtherClaimNullValueReturnsNull() {
        JWK jwk = new JWK();
        jwk.setOtherClaims("crv", null);

        String result = jwk.getOtherClaim("crv", String.class);

        assertNull(result);
    }

    @Test
    public void getOtherClaimIntegerValueRetrievedAsInteger() {
        JWK jwk = new JWK();
        jwk.setOtherClaims("x", 42);

        Integer result = jwk.getOtherClaim("x", Integer.class);

        assertEquals(Integer.valueOf(42), result);
    }
}
