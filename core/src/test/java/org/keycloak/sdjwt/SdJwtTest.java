package org.keycloak.sdjwt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Optional;

import org.junit.Test;
import org.keycloak.crypto.SignatureSignerContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJwtTest {

    @Test
    public void testToSdJwtString() {
        // Test the toSdJwtString method with different scenarios:
        // - IssuerSignedJWT and no claims, no keyBindingJWT
        // - IssuerSignedJWT with claims and no keyBindingJWT
        // - IssuerSignedJWT with claims and keyBindingJWT
        // Verify the format and correctness of the output string
    }

    @Test
    public void testToString() {
        // Test that toString returns the correct serialized JWT
        // Test that calling toString multiple times returns the same string (to verify
        // caching)
    }

    // Additional tests can be written to cover edge cases, error conditions,
    // and any other functionality specific to the SdJwt class.
    @Test
    public void testIssuerSignedJWTWithUndiclosedClaims3_3() {
        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
                .withUndisclosedClaim("given_name", "2GLC42sKQveCfGfryNRN9w")
                .withUndisclosedClaim("family_name", "eluV5Og3gSNII8EYnsxA_A")
                .withUndisclosedClaim("email", "6Ij7tM-a5iVPGboS5tmvVA")
                .withUndisclosedClaim("phone_number", "eI8ZWm9QnKPpNPeNenHdhQ")
                .withUndisclosedClaim("address", "Qg_O64zqAxe412a108iroA")
                .withUndisclosedClaim("birthdate", "AJx-095VPrpTtN4QMOqROA")
                .withUndisclosedClaim("is_over_18", "Pc33JM2LchcU_lHggv_ufQ")
                .withUndisclosedClaim("is_over_21", "G02NSrQfjFXQ7Io09syajA")
                .withUndisclosedClaim("is_over_65", "lklxF5jMYlGTPUovMNIvCA")
                .build();

        // Read claims provided by the holder
        JsonNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-holder-claims.json");
        // Read claims added by the issuer
        JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-issuer-claims.json");

        // Merge both
        ((ObjectNode) holderClaimSet).setAll((ObjectNode) issuerClaimSet);

        SdJwt sdJwt = new SdJwt(disclosureSpec, holderClaimSet, Optional.empty(),
                TestSettings.getInstance().getIssuerSignerContext());
        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();

        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s3.3-issuer-payload.json");
        assertEquals(expected, jwt.getPayload());

        String sdJwtString = sdJwt.toSdJwtString();

        SdJwt actualSdJwt = new SdJwt(sdJwtString);

        String expectedString = TestUtils.readFileAsString(getClass(), "sdjwt/s3.3-unsecured-sd-jwt.txt");
        SdJwt expecteSdJwt = new SdJwt(expectedString);

        TestCompareSdJwt.compare(expecteSdJwt, actualSdJwt);

    }

    @Test
    public void settingsTest() {
        SignatureSignerContext issuerSignerContext = TestSettings.getInstance().getIssuerSignerContext();
        assertNotNull(issuerSignerContext);
    }
}
