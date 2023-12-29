package org.keycloak.sdjwt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author <a href="mailto:francis.pouatcha@adorsys.com">Francis Pouatcha</a>
 */
public class SdJWTSamplesTests {
    @Test
    public void testS7_1_FlatSdJwt() {
        // Read claims provided by the holder
        JsonNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-holder-claims.json");
        // Read claims added by the issuer
        JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-issuer-claims.json");
        ((ObjectNode) holderClaimSet).setAll((ObjectNode) issuerClaimSet);

        // produce the main sdJwt, adding nested sdJwts
        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
            .withUndisclosedClaim("address", "2GLC42sKQveCfGfryNRN9w")
            .build();
        SdJwt sdJwt = SdJwt.builder()
            .withDisclosureSpec(disclosureSpec)
            .withClaimSet(holderClaimSet)
            .build();

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();
        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s7.1-issuer-payload.json");

        assertEquals(expected, jwt.getPayload());
    }

    @Test
    public void testS7_2_StructuredSdJwt() {
        // Read claims provided by the holder
        JsonNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-holder-claims.json");
        // Read claims added by the issuer
        JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-issuer-claims.json");
        ((ObjectNode) holderClaimSet).setAll((ObjectNode) issuerClaimSet);

        DisclosureSpec addrDisclosureSpec = DisclosureSpec.builder()
        .withUndisclosedClaim("street_address", "2GLC42sKQveCfGfryNRN9w")
            .withUndisclosedClaim("locality", "eluV5Og3gSNII8EYnsxA_A")
            .withUndisclosedClaim("region", "6Ij7tM-a5iVPGboS5tmvVA")
            .withUndisclosedClaim("country", "eI8ZWm9QnKPpNPeNenHdhQ")
            .build();

        // Read claims provided by the holder
        JsonNode addressClaimSet = holderClaimSet.get("address");
        // produce the nested sdJwt
        SdJwt addrSdJWT = SdJwt.builder()
            .withDisclosureSpec(addrDisclosureSpec)
            .withClaimSet(addressClaimSet)
            .build();
        // cleanup e.g nested _sd_alg
        JsonNode addPayload = addrSdJWT.asNestedPayload();
        // Set payload back into main claim set
        ((ObjectNode) holderClaimSet).set("address", addPayload);

        DisclosureSpec disclosureSpec = DisclosureSpec.builder().build();
        // produce the main sdJwt, adding nested sdJwts
        SdJwt sdJwt = SdJwt.builder()
            .withDisclosureSpec(disclosureSpec)
            .withClaimSet(holderClaimSet)
            .withNestedSdJwt(addrSdJWT)
            .build();

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();
        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s7.2-issuer-payload.json");
        assertEquals(expected, jwt.getPayload());

    }

    @Test
    public void testS7_2b_PartialDisclosureOfStructuredSdJwt() {
        // Read claims provided by the holder
        JsonNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-holder-claims.json");
        // Read claims added by the issuer
        JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-issuer-claims.json");
        ((ObjectNode) holderClaimSet).setAll((ObjectNode) issuerClaimSet);

        DisclosureSpec addrDisclosureSpec = DisclosureSpec.builder()
            .withUndisclosedClaim("street_address", "2GLC42sKQveCfGfryNRN9w")
            .withUndisclosedClaim("locality", "eluV5Og3gSNII8EYnsxA_A")
            .withUndisclosedClaim("region", "6Ij7tM-a5iVPGboS5tmvVA")
            .build();

        // Read claims provided by the holder
        JsonNode addressClaimSet = holderClaimSet.get("address");
        // produce the nested sdJwt
        SdJwt addrSdJWT = SdJwt.builder()
            .withDisclosureSpec(addrDisclosureSpec)
            .withClaimSet(addressClaimSet)
            .build();
        // cleanup e.g nested _sd_alg
        JsonNode addPayload = addrSdJWT.asNestedPayload();
        // Set payload back into main claim set
        ((ObjectNode) holderClaimSet).set("address", addPayload);

        DisclosureSpec disclosureSpec = DisclosureSpec.builder().build();
        // produce the main sdJwt, adding nested sdJwts
        SdJwt sdJwt = SdJwt.builder()
            .withDisclosureSpec(disclosureSpec)
            .withClaimSet(holderClaimSet)
            .withNestedSdJwt(addrSdJWT)
            .build();

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();
        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s7.2b-issuer-payload.json");
        assertEquals(expected, jwt.getPayload());

    }

    @Test
    public void testS7_3_RecursiveDisclosureOfStructuredSdJwt() {
        // Read claims provided by the holder
        JsonNode holderClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-holder-claims.json");
        // Read claims added by the issuer
        JsonNode issuerClaimSet = TestUtils.readClaimSet(getClass(), "sdjwt/s7-issuer-claims.json");
        ((ObjectNode) holderClaimSet).setAll((ObjectNode) issuerClaimSet);

        DisclosureSpec addrDisclosureSpec = DisclosureSpec.builder()
            .withUndisclosedClaim("street_address", "2GLC42sKQveCfGfryNRN9w")
            .withUndisclosedClaim("locality", "eluV5Og3gSNII8EYnsxA_A")
            .withUndisclosedClaim("region", "6Ij7tM-a5iVPGboS5tmvVA")
            .withUndisclosedClaim("country", "eI8ZWm9QnKPpNPeNenHdhQ")
            .build();

        // Read claims provided by the holder
        JsonNode addressClaimSet = holderClaimSet.get("address");
        // produce the nested sdJwt
        SdJwt addrSdJWT = SdJwt.builder()
            .withDisclosureSpec(addrDisclosureSpec)
            .withClaimSet(addressClaimSet)
            .build();
        // cleanup e.g nested _sd_alg
        JsonNode addPayload = addrSdJWT.asNestedPayload();
        // Set payload back into main claim set
        ((ObjectNode) holderClaimSet).set("address", addPayload);

        DisclosureSpec disclosureSpec = DisclosureSpec.builder()
            .withUndisclosedClaim("address", "Qg_O64zqAxe412a108iroA")
            .build();
        // produce the main sdJwt, adding nested sdJwts
        SdJwt sdJwt = SdJwt.builder()
            .withDisclosureSpec(disclosureSpec)
            .withClaimSet(holderClaimSet)
            .withNestedSdJwt(addrSdJWT)
            .build();

        IssuerSignedJWT jwt = sdJwt.getIssuerSignedJWT();
        JsonNode expected = TestUtils.readClaimSet(getClass(), "sdjwt/s7.3-issuer-payload.json");
        assertEquals(expected, jwt.getPayload());
    }
}
